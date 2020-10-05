package no.nav.sifinnsynapi.common

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.security.token.support.test.spring.TokenGeneratorConfiguration
import no.nav.sifinnsynapi.Routes.SØKNAD
import no.nav.sifinnsynapi.dokument.DokumentService
import no.nav.sifinnsynapi.http.SøknadNotFoundException
import no.nav.sifinnsynapi.soknad.SøknadController
import no.nav.sifinnsynapi.soknad.SøknadDTO
import no.nav.sifinnsynapi.soknad.SøknadService
import no.nav.sifinnsynapi.util.CallIdGenerator
import no.nav.sifinnsynapi.utils.tokenSomHttpHeader
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*


@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(TokenGeneratorConfiguration::class, CallIdGenerator::class) // Tilgjengliggjør en oicd-provider for test. Se application-test.yml -> no.nav.security.jwt.issuer.selvbetjening for konfigurasjon
@WebMvcTest(
        controllers = [SøknadController::class],
        properties = [
            "spring.cloud.gcp.core.enabled=false",
            "spring.cloud.gcp.secretmanager.enabled=false",
            "no.nav.security.jwt.issuer.selvbetjening.discoveryUrl=http://metadata",
            "no.nav.security.jwt.issuer.selvbetjening.accepted_audience=aud-localhost",
            "no.nav.security.jwt.issuer.selvbetjening.cookie_name=localhost-idtoken"
        ])
class SøknadControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var søknadService: SøknadService

    @MockkBean
    lateinit var dokumentService: DokumentService

    companion object {
        private val fødselsnummer = Fødselsnummer.valueOf("1234567")
        private val authorizationHeader = tokenSomHttpHeader(fødselsnummer)
    }

    @Test
    fun `internal server error gir 500 med forventet problem-details`() {
        every {
            søknadService.hentSøknader()
        } throws Exception("Ooops, noe gikk galt...")

        mockMvc.perform(MockMvcRequestBuilders
                .get(URI(URLDecoder.decode(SØKNAD, Charset.defaultCharset())))
                .accept(MediaType.APPLICATION_JSON)
                .headers(authorizationHeader)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isInternalServerError)
                .andExpect(jsonPath("$.type").value("/problem-details/internal-server-error"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
    }

    @Test
    fun `internal server error gir 500 med forventet problem-details i header`() {
        every {
            søknadService.hentSøknader()
        } throws Exception("Ooops, noe gikk galt...")

        mockMvc.perform(MockMvcRequestBuilders
                .get(URI(URLDecoder.decode(SØKNAD, Charset.defaultCharset())))
                .accept(MediaType.APPLICATION_JSON)
                .headers(authorizationHeader)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isInternalServerError)
                .andExpect(header().exists("problem-details"))
                .andExpect(header().string(
                        "problem-details",
                        //language=json
                        """{"type":"/problem-details/internal-server-error","title":"Internal Server Error","status":500,"detail":"Ooops, noe gikk galt..."}""".trimIndent()))
    }

    @Test
    fun `uautorisert kall gir 401 med forventet problem-details`() {
        mockMvc.perform(MockMvcRequestBuilders
                .get(URI(URLDecoder.decode(SØKNAD, Charset.defaultCharset())))
                .accept(MediaType.APPLICATION_JSON)
                // .headers(authorizationHeader)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.type").value("/problem-details/uautentisert-forespørsel"))
                .andExpect(jsonPath("$.title").value("Ikke autentisert"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("no.nav.security.token.support.core.exceptions.JwtTokenMissingException: no valid token found in validation context"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
    }

    @Test
    fun `Gitt 200 respons, forvent korrekt format på liste av søknader`() {
        every {
            søknadService.hentSøknader()
        } returns listOf(
                SøknadDTO(
                        søknadId = UUID.randomUUID(),
                        saksId = "abc123",
                        søknadstype = Søknadstype.OMP_UTBETALING_SNF,
                        status = SøknadsStatus.MOTTATT,
                        journalpostId = "123456789",
                        opprettet = ZonedDateTime.parse("2020-08-04T10:30:00Z").withZoneSameInstant(ZoneId.of("UTC")),
                        søknad = mapOf(
                                "soknadId" to UUID.randomUUID().toString(),
                                "mottatt" to ZonedDateTime.now(),
                                "søker" to mapOf(
                                        "fødselsnummer" to "1234567",
                                        "aktørId" to AktørId.valueOf("123456")
                                )
                        )
                )
        )

        mockMvc.perform(MockMvcRequestBuilders
                .get(URI(URLDecoder.decode(SØKNAD, Charset.defaultCharset())))
                .accept(MediaType.APPLICATION_JSON)
                .headers(authorizationHeader)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("[0].saksId").isString)
                .andExpect(jsonPath("[0].saksId").value("abc123"))
                .andExpect(jsonPath("[0].søknadstype").isString)
                .andExpect(jsonPath("[0].søknadstype").value("OMP_UTBETALING_SNF"))
                .andExpect(jsonPath("[0].status").isString)
                .andExpect(jsonPath("[0].status").value("MOTTATT"))
                .andExpect(jsonPath("[0].journalpostId").isString)
                .andExpect(jsonPath("[0].journalpostId").value("123456789"))
                .andExpect(jsonPath("[0].opprettet").isString)
                .andExpect(jsonPath("[0].opprettet").value("2020-08-04T10:30:00.000Z"))
                .andExpect(jsonPath("[0].endret").doesNotExist())
                .andExpect(jsonPath("[0].behandlingsdato").doesNotExist())
                .andExpect(jsonPath("[0].søknad").isMap)
    }

    @Test
    fun `Gitt 200 respons, forvent korrekt format ved henting av søknad`() {
        val søknadId = UUID.randomUUID()
        every {
            søknadService.hentSøknad(any())
        } returns
                SøknadDTO(
                        søknadId = søknadId,
                        saksId = "abc123",
                        søknadstype = Søknadstype.OMP_UTBETALING_SNF,
                        status = SøknadsStatus.MOTTATT,
                        journalpostId = "123456789",
                        opprettet = ZonedDateTime.parse("2020-08-04T10:30:00Z").withZoneSameInstant(ZoneId.of("UTC")),
                        søknad = mapOf(
                                "soknadId" to søknadId.toString(),
                                "mottatt" to ZonedDateTime.now(),
                                "søker" to mapOf(
                                        "fødselsnummer" to "1234567",
                                        "aktørId" to AktørId.valueOf("123456")
                                )
                        )
                )

        mockMvc.perform(MockMvcRequestBuilders
                .get(URI(URLDecoder.decode("${SØKNAD}/$søknadId", Charset.defaultCharset())))
                .accept(MediaType.APPLICATION_JSON)
                .headers(authorizationHeader)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.saksId").isString)
                .andExpect(jsonPath("$.saksId").value("abc123"))
                .andExpect(jsonPath("$.søknadstype").isString)
                .andExpect(jsonPath("$.søknadstype").value("OMP_UTBETALING_SNF"))
                .andExpect(jsonPath("$.status").isString)
                .andExpect(jsonPath("$.status").value("MOTTATT"))
                .andExpect(jsonPath("$.journalpostId").isString)
                .andExpect(jsonPath("$.journalpostId").value("123456789"))
                .andExpect(jsonPath("$.opprettet").isString)
                .andExpect(jsonPath("$.opprettet").value("2020-08-04T10:30:00.000Z"))
                .andExpect(jsonPath("$.endret").doesNotExist())
                .andExpect(jsonPath("$.behandlingsdato").doesNotExist())
                .andExpect(jsonPath("$.søknad").isMap)
    }

    @Test
    fun `gitt at søknad ikke blir funnet, forvent status 404 med problem-details`() {
        val søknadId = UUID.randomUUID()
        every { søknadService.hentSøknad(any()) } throws SøknadNotFoundException(søknadId.toString())

        mockMvc.perform(MockMvcRequestBuilders
                .get(URI(URLDecoder.decode("${SØKNAD}/$søknadId", Charset.defaultCharset())))
                .accept(MediaType.APPLICATION_JSON)
                .headers(authorizationHeader)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.type").value("/problem-details/søknad-ikke-funnet"))
                .andExpect(jsonPath("$.title").value("Søknad ikke funnet"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Søknad med søknadId = $søknadId ble ikke funnet."))
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
    }
}

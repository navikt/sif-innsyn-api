package no.nav.sifinnsynapi.common

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.security.token.support.test.spring.TokenGeneratorConfiguration
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
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

    companion object {
        private val fødselsnummer = Fødselsnummer.valueOf("1234567")
        private val authorizationHeader = tokenSomHttpHeader(fødselsnummer)
    }

    @Test
    fun `internal server error gir 500 med forventet problem-details`() {
        every {
            søknadService.hentSøknad()
        } throws Exception("Ooops, noe gikk galt...")

        mockMvc.perform(MockMvcRequestBuilders
                .get("/soknad")
                .accept(MediaType.APPLICATION_JSON)
                .headers(authorizationHeader)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isInternalServerError)
                .andExpect(jsonPath("$.type").value("/problem-details/internal-server-error"))
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
    }

    @Test
    fun `uautorisert kall gir 401 med forventet problem-details`() {
        mockMvc.perform(MockMvcRequestBuilders
                .get("/soknad")
                .accept(MediaType.APPLICATION_JSON)
                // .headers(authorizationHeader)
        )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnauthorized)
                .andExpect(jsonPath("$.type").value("/problem-details/unauthorized"))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").value("Ikke autentisert: Forespørsel med gitt token er ikke autentisert."))
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
    }

    @Test
    fun `Gitt 200 respons, forvent korrekt format på liste av søknader`() {
        every {
            søknadService.hentSøknad()
        } returns listOf(
                SøknadDTO(
                        saksId = "abc123",
                        søknadstype = Søknadstype.OMP_UTBETALING_SNF,
                        status = SøknadsStatus.MOTTATT,
                        journalpostId = "123456789",
                        opprettet = LocalDateTime.parse("2020-08-04T10:30:00"),
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
                .get("/soknad")
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
                .andExpect(jsonPath("[0].opprettet").value("2020-08-04T10:30:00"))
                .andExpect(jsonPath("[0].endret").doesNotExist())
                .andExpect(jsonPath("[0].behandlingsdato").doesNotExist())
                .andExpect(jsonPath("[0].søknad").isMap)
    }
}

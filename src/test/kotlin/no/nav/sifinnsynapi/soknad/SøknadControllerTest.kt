package no.nav.sifinnsynapi.soknad

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import jakarta.servlet.http.Cookie
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.sifinnsynapi.Routes.SØKNAD
import no.nav.sifinnsynapi.common.AktørId
import no.nav.sifinnsynapi.common.SøknadsStatus
import no.nav.sifinnsynapi.common.Søknadstype
import no.nav.sifinnsynapi.config.Issuers
import no.nav.sifinnsynapi.config.SecurityConfiguration
import no.nav.sifinnsynapi.dokument.DokumentService
import no.nav.sifinnsynapi.oppslag.TilgangNektetException
import no.nav.sifinnsynapi.util.CallIdGenerator
import no.nav.sifinnsynapi.utils.hentToken
import org.junit.Assert.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpHeaders.CONTENT_DISPOSITION
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.Charset
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*


@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnableMockOAuth2Server // Tilgjengliggjør en oicd-provider for test.
@Import(CallIdGenerator::class, SecurityConfiguration::class)
@WebMvcTest(controllers = [SøknadController::class])
@ActiveProfiles("test")
class SøknadControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    lateinit var mockOAuth2Server: MockOAuth2Server

    @MockkBean(relaxed = true)
    lateinit var søknadService: SøknadService

    @MockkBean
    lateinit var dokumentService: DokumentService

    @BeforeAll
    internal fun setUp() {
        assertNotNull(mockOAuth2Server)
    }

    @Test
    fun `internal server error gir 500 med forventet problem-details`() {
        every {
            søknadService.hentSøknader()
        } throws Exception("Ooops, noe gikk galt...")

        mockMvc.perform(
            MockMvcRequestBuilders
                .get(URI(URLDecoder.decode(SØKNAD, Charset.defaultCharset())))
                .accept(MediaType.APPLICATION_JSON)
                .cookie(Cookie("selvbetjening-idtoken", mockOAuth2Server.hentToken().serialize()))
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

        mockMvc.perform(
            MockMvcRequestBuilders
                .get(URI(URLDecoder.decode(SØKNAD, Charset.defaultCharset())))
                .accept(MediaType.APPLICATION_JSON)
                .cookie(Cookie("selvbetjening-idtoken", mockOAuth2Server.hentToken().serialize()))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isInternalServerError)
            .andExpect(header().exists("problem-details"))
            .andExpect(
                header().string(
                    "problem-details",
                    //language=json
                    """{"type":"/problem-details/internal-server-error","title":"Et uventet feil har oppstått","status":500,"detail":"Ooops, noe gikk galt...","instance":"http://localhost/soknad"}""".trimIndent()
                )
            )
    }

    @Test
    fun `Gitt 200 respons, forvent korrekt format på liste av søknader`() {
        every {
            søknadService.hentSøknader()
        } returns listOf(
            SøknadDTO(
                søknadId = UUID.randomUUID(),
                saksId = "abc123",
                søknadstype = Søknadstype.PP_SYKT_BARN,
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
                ),
                dokumenter = listOf()
            )
        )

        mockMvc.perform(
            MockMvcRequestBuilders
                .get(URI(URLDecoder.decode(SØKNAD, Charset.defaultCharset())))
                .accept(MediaType.APPLICATION_JSON)
                .cookie(Cookie("selvbetjening-idtoken", mockOAuth2Server.hentToken().serialize()))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("[0].saksId").isString)
            .andExpect(jsonPath("[0].saksId").value("abc123"))
            .andExpect(jsonPath("[0].søknadstype").isString)
            .andExpect(jsonPath("[0].søknadstype").value("PP_SYKT_BARN"))
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
    fun `Gitt at søknad blir funnet, forvent korrekt format på sist mottatte PSB søknad`() {
        every {
            søknadService.hentSistInnsendteSøknad(any())
        } returns
                SøknadDTO(
                    søknadId = UUID.randomUUID(),
                    saksId = "abc123",
                    søknadstype = Søknadstype.PP_SYKT_BARN,
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
                    ),
                    dokumenter = listOf()
                )

        mockMvc.perform(
            MockMvcRequestBuilders
                .get(URI(URLDecoder.decode("$SØKNAD/psb/siste", Charset.defaultCharset())))
                .accept(MediaType.APPLICATION_JSON)
                .cookie(Cookie("selvbetjening-idtoken", mockOAuth2Server.hentToken().serialize()))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.saksId").isString)
            .andExpect(jsonPath("$.saksId").value("abc123"))
            .andExpect(jsonPath("$.søknadstype").isString)
            .andExpect(jsonPath("$.søknadstype").value("PP_SYKT_BARN"))
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
    fun `Gitt at søknad ikke blir funnet, forvent korrekt format på sist mottatte PSB søknad`() {
        every {
            søknadService.hentSistInnsendteSøknad(any())
        } returns null

        mockMvc.perform(
            MockMvcRequestBuilders
                .get(URI(URLDecoder.decode("$SØKNAD/psb/siste", Charset.defaultCharset())))
                .accept(MediaType.APPLICATION_JSON)
                .cookie(Cookie("selvbetjening-idtoken", mockOAuth2Server.hentToken().serialize()))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isNotFound)
    }

    @Test
    fun `Gitt 200 respons, forvent korrekt format på liste av søknader med tokenx token`() {
        every {
            søknadService.hentSøknader()
        } returns listOf(
            SøknadDTO(
                søknadId = UUID.randomUUID(),
                saksId = "abc123",
                søknadstype = Søknadstype.PP_SYKT_BARN,
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
                ),
                dokumenter = listOf()
            )
        )

        mockMvc.perform(
            MockMvcRequestBuilders
                .get(URI(URLDecoder.decode(SØKNAD, Charset.defaultCharset())))
                .accept(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, "Bearer ${mockOAuth2Server.hentToken(issuerId = Issuers.TOKEN_X).serialize()}")
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("[0].saksId").isString)
            .andExpect(jsonPath("[0].saksId").value("abc123"))
            .andExpect(jsonPath("[0].søknadstype").isString)
            .andExpect(jsonPath("[0].søknadstype").value("PP_SYKT_BARN"))
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
                    søknadstype = Søknadstype.PP_SYKT_BARN,
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
                    ),
                    dokumenter = listOf()
                )

        mockMvc.perform(
            MockMvcRequestBuilders
                .get(URI(URLDecoder.decode("${SØKNAD}/$søknadId", Charset.defaultCharset())))
                .accept(MediaType.APPLICATION_JSON)
                .cookie(Cookie("selvbetjening-idtoken", mockOAuth2Server.hentToken().serialize()))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.saksId").isString)
            .andExpect(jsonPath("$.saksId").value("abc123"))
            .andExpect(jsonPath("$.søknadstype").isString)
            .andExpect(jsonPath("$.søknadstype").value("PP_SYKT_BARN"))
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
    fun `Gitt 451 feil ved oppsalg av aktørId, forvent tilgang nektet problem detail med 451 statuskode`() {

        UUID.randomUUID()
        every { søknadService.hentSøknader() } throws TilgangNektetException()

        mockMvc.perform(
            MockMvcRequestBuilders
                .get(URI(URLDecoder.decode("$SØKNAD", Charset.defaultCharset())))
                .accept(MediaType.APPLICATION_JSON)
                .cookie(Cookie("selvbetjening-idtoken", mockOAuth2Server.hentToken().serialize()))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isUnavailableForLegalReasons)
            .andExpect(jsonPath("$.type").value("/problem-details/tilgang-nektet"))
            .andExpect(jsonPath("$.title").value("Tilgang til person nektet"))
            .andExpect(jsonPath("$.detail").value("Tilgang til personen ble nektet fordi personen enten er under 18 år eller ikke i live."))
            .andExpect(jsonPath("$.status").value(451))
            .andExpect(jsonPath("$.stackTrace").doesNotExist())
    }

    @Test
    fun `gitt at søknad ikke blir funnet, forvent status 404 med problem-details`() {
        val søknadId = UUID.randomUUID()
        every { søknadService.hentSøknad(any()) } throws SøknadNotFoundException(søknadId.toString())

        mockMvc.perform(
            MockMvcRequestBuilders
                .get(URI(URLDecoder.decode("${SØKNAD}/$søknadId", Charset.defaultCharset())))
                .accept(MediaType.APPLICATION_JSON)
                .cookie(Cookie("selvbetjening-idtoken", mockOAuth2Server.hentToken().serialize()))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.type").value("/problem-details/søknad-ikke-funnet"))
            .andExpect(jsonPath("$.title").value("Søknad ble ikke funnet"))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.stackTrace").doesNotExist())
    }

    @Test
    fun `gitt request uten token, forevnt 401`() {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get(URI(URLDecoder.decode(SØKNAD, Charset.defaultCharset())))
                .accept(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.type").value("/problem-details/uautentisert-forespørsel"))
            .andExpect(jsonPath("$.title").value("Ikke autentisert"))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.stackTrace").doesNotExist())
    }

    @Test
    fun `gitt request med token utsedt av annen issuer, forevnt 401`() {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get(URI(URLDecoder.decode(SØKNAD, Charset.defaultCharset())))
                .accept(MediaType.APPLICATION_JSON)
                .cookie(
                    Cookie(
                        "selvbetjening-idtoken",
                        mockOAuth2Server.hentToken(issuerId = "ukjent issuer").serialize()
                    )
                )
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.type").value("/problem-details/uautentisert-forespørsel"))
            .andExpect(jsonPath("$.title").value("Ikke autentisert"))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.stackTrace").doesNotExist())
    }

    @Test
    fun `gitt request med utgått token, forevnt 401`() {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get(URI(URLDecoder.decode(SØKNAD, Charset.defaultCharset())))
                .accept(MediaType.APPLICATION_JSON)
                .header(
                    AUTHORIZATION,
                    "Bearer ${mockOAuth2Server.hentToken(issuerId = Issuers.TOKEN_X, expiry = -100).serialize()}"
                )
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.type").value("/problem-details/uautentisert-forespørsel"))
            .andExpect(jsonPath("$.title").value("Ikke autentisert"))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.stackTrace").doesNotExist())
    }

    @Test
    fun `gitt request med token med ukjent audience, forevnt 401`() {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get(URI(URLDecoder.decode(SØKNAD, Charset.defaultCharset())))
                .accept(MediaType.APPLICATION_JSON)
                .cookie(
                    Cookie(
                        "selvbetjening-idtoken",
                        mockOAuth2Server.hentToken(audience = "ukjent audience").serialize()
                    )
                )
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.type").value("/problem-details/uautentisert-forespørsel"))
            .andExpect(jsonPath("$.title").value("Ikke autentisert"))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.stackTrace").doesNotExist())
    }

    @Test
    fun `gitt request med token med acr=Level3, forevnt 401`() {
        mockMvc.perform(
            MockMvcRequestBuilders
                .get(URI(URLDecoder.decode(SØKNAD, Charset.defaultCharset())))
                .accept(MediaType.APPLICATION_JSON)
                .cookie(
                    Cookie(
                        "selvbetjening-idtoken",
                        mockOAuth2Server.hentToken(claims = mapOf("acr" to "Level3")).serialize()
                    )
                )
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.type").value("/problem-details/uautentisert-forespørsel"))
            .andExpect(jsonPath("$.title").value("Ikke autentisert"))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.stackTrace").doesNotExist())
    }

    @Test
    fun `forvent generert filnavn med mellomrom`() {
        every {
            søknadService.hentArbeidsgiverMeldingFil(any(), any())
        } returns "some byteArray".toByteArray()

        val forventetFilnavn = "Bekreftelse_til_arbeidsgiver_12345678.pdf"
        mockMvc.perform(
            MockMvcRequestBuilders
                .get(
                    URI(
                        URLDecoder.decode(
                            "$SØKNAD/${UUID.randomUUID()}/arbeidsgivermelding",
                            Charset.defaultCharset()
                        )
                    )
                )
                .queryParam("organisasjonsnummer", "12345678")
                .accept(MediaType.APPLICATION_PDF_VALUE)
                .cookie(Cookie("selvbetjening-idtoken", mockOAuth2Server.hentToken().serialize()))
        )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(status().isOk)
            .andExpect(header().exists(CONTENT_DISPOSITION))
            .andExpect(header().string(CONTENT_DISPOSITION, "inline; filename=$forventetFilnavn"))
    }
}

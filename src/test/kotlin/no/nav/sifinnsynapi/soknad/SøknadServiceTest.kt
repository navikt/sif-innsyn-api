package no.nav.sifinnsynapi.soknad

import assertk.assertThat
import assertk.assertions.*
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.sifinnsynapi.common.AktørId
import no.nav.sifinnsynapi.common.Fødselsnummer
import no.nav.sifinnsynapi.common.SøknadsStatus
import no.nav.sifinnsynapi.common.Søknadstype
import no.nav.sifinnsynapi.dokument.DokumentService
import no.nav.sifinnsynapi.oppslag.OppslagRespons
import no.nav.sifinnsynapi.oppslag.OppslagsService
import org.awaitility.kotlin.await
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
@ActiveProfiles("test")
@EnableMockOAuth2Server // Tilgjengliggjør en oicd-provider for test.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK) // Integrasjonstest - Kjører opp hele Spring Context med alle konfigurerte beans.
internal class SøknadServiceTest {

    @Autowired
    private lateinit var søknadRepository: SøknadRepository

    @Autowired
    private lateinit var søknadService: SøknadService

    @MockkBean
    private lateinit var dokumentService: DokumentService

    @MockkBean
    private lateinit var oppslagsService: OppslagsService

    @BeforeEach
    internal fun setUp() {
        every { oppslagsService.hentAktørId() } returns OppslagRespons("123456")
    }

    @AfterAll
    internal fun tearDown() {
        søknadRepository.deleteAll()
    }

    @Test
    fun hentArbeidsgiverMeldingFil() {
        val organisasjonsnummer = "917755645"
        val søknad = søknadRepository.save(
            SøknadDAO(
                //id = søknadId,
                saksId = "abc123",
                søknadstype = Søknadstype.PP_SYKT_BARN,
                status = SøknadsStatus.MOTTATT,
                journalpostId = "123456789",
                opprettet = ZonedDateTime.parse("2020-08-04T10:30:00Z").withZoneSameInstant(ZoneId.of("UTC")),
                fødselsnummer = Fødselsnummer("02119970078"),
                aktørId = AktørId("123456"),
                søknad =
                //language=json
                """
                            {
                                "fraOgMed": "2021-01-01",
                                "tilOgMed": "2021-01-01",
                                "søker": {
                                  "mellomnavn": "Mellomnavn",
                                  "etternavn": "Nordmann",
                                  "aktørId": "123456",
                                  "fødselsnummer": "02119970078",
                                  "fornavn": "Ola"
                                },
                                "arbeidsgivere": {
                                  "organisasjoner": [
                                    {
                                      "navn": "Stolt Hane",
                                      "organisasjonsnummer": "917755736"
                                    },
                                    {
                                      "navn": "Snill Torpedo",
                                      "organisasjonsnummer": "$organisasjonsnummer"
                                    },
                                    {
                                      "navn": "Something Fishy",
                                      "organisasjonsnummer": "917755645"
                                    }
                                  ]
                                }
                            }
                        """.trimIndent()
            )
        )

        await.atMost(Duration.ofSeconds(10)).ignoreException(SøknadNotFoundException::class.java).untilAsserted {
            assertThat(søknadRepository.findById(søknad.id)).isNotEqualTo(Optional.empty<Any?>())
            val bytes = søknadService.hentArbeidsgiverMeldingFil(søknad.id, organisasjonsnummer)
            assertNotNull(bytes)
            assertThat(bytes).isNotEmpty()
            assertThat(bytes).size().isGreaterThan(1000)
        }
    }

    @Test
    fun `Finner organisasjon selvom arbeidsgivere ligger som JSONArray`() {
        val organisasjonsnummer = "917755645"
        val søknad = søknadRepository.save(
            SøknadDAO(
                saksId = "abc123",
                søknadstype = Søknadstype.PP_SYKT_BARN,
                status = SøknadsStatus.MOTTATT,
                journalpostId = "123456789",
                opprettet = ZonedDateTime.parse("2020-08-04T10:30:00Z").withZoneSameInstant(ZoneId.of("UTC")),
                fødselsnummer = Fødselsnummer("02119970078"),
                aktørId = AktørId("123456"),
                søknad =
                //language=json
                """
                {
                  "fraOgMed": "2021-01-01",
                  "tilOgMed": "2021-01-01",
                  "søker": {
                    "mellomnavn": "Mellomnavn",
                    "etternavn": "Nordmann",
                    "aktørId": "123456",
                    "fødselsnummer": "02119970078",
                    "fornavn": "Ola"
                  },
                  "arbeidsgivere": [
                    {
                      "erAnsatt": true,
                      "navn": "Peppes",
                      "arbeidsforhold": null,
                      "organisasjonsnummer": "917755645"
                    },
                    {
                      "erAnsatt": true,
                      "navn": "Mix",
                      "arbeidsforhold": null,
                      "organisasjonsnummer": "896967762"
                    }
                  ]
                }
                """.trimIndent()
            )
        )

        await.atMost(Duration.ofSeconds(10)).ignoreException(SøknadNotFoundException::class.java).untilAsserted {
            assertThat(søknadRepository.findById(søknad.id)).isNotEqualTo(Optional.empty<Any?>())
            val bytes = søknadService.hentArbeidsgiverMeldingFil(søknad.id, organisasjonsnummer)
            assertNotNull(bytes)
            assertThat(bytes).isNotEmpty()
            assertThat(bytes).size().isGreaterThan(1000)
        }
    }

    @Test
    fun `gitt henting av dokumentoversikt feiler, forvent at søknader fortsatt hentes`() {
        søknadRepository.save(
            SøknadDAO(
                saksId = "abc123",
                søknadstype = Søknadstype.PP_SYKT_BARN,
                status = SøknadsStatus.MOTTATT,
                journalpostId = "123456789",
                opprettet = ZonedDateTime.parse("2020-08-04T10:30:00Z").withZoneSameInstant(ZoneId.of("UTC")),
                fødselsnummer = Fødselsnummer("02119970078"),
                aktørId = AktørId("123456"),
                søknad =
                //language=json
                """
                {}
                """.trimIndent()
            )
        )

        every { dokumentService.hentDokumentOversikt(any()) } throws IllegalStateException("Feilet med å hente dokumentoversikt")

        val søknader = søknadService.hentSøknader()
        assertThat(søknader).isNotEmpty()
        assertThat(søknader.first().dokumenter).isEmpty()

    }
}

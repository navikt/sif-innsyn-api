package no.nav.sifinnsynapi.konsument

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.coEvery
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.sifinnsynapi.Routes.SØKNAD
import no.nav.sifinnsynapi.SifInnsynApiApplication
import no.nav.sifinnsynapi.common.AktørId
import no.nav.sifinnsynapi.common.Fødselsnummer
import no.nav.sifinnsynapi.common.SøknadsStatus
import no.nav.sifinnsynapi.common.Søknadstype
import no.nav.sifinnsynapi.config.SecurityConfiguration
import no.nav.sifinnsynapi.config.Topics.AAPEN_DOK_JOURNALFØRING_V1
import no.nav.sifinnsynapi.config.Topics.K9_DITTNAV_VARSEL_BESKJED
import no.nav.sifinnsynapi.config.Topics.K9_DITTNAV_VARSEL_BESKJED_AIVEN
import no.nav.sifinnsynapi.config.Topics.PP_SYKT_BARN
import no.nav.sifinnsynapi.dittnav.K9Beskjed
import no.nav.sifinnsynapi.konsument.omsorgspenger.utbetaling.arbeidstaker.OmsorgspengerutbetalingArbeidstakerHendelseKonsument
import no.nav.sifinnsynapi.safselvbetjening.SafSelvbetjeningService
import no.nav.sifinnsynapi.safselvbetjening.generated.enums.Datotype
import no.nav.sifinnsynapi.safselvbetjening.generated.enums.Journalstatus
import no.nav.sifinnsynapi.safselvbetjening.generated.enums.Variantformat
import no.nav.sifinnsynapi.safselvbetjening.generated.hentdokumentoversikt.*
import no.nav.sifinnsynapi.soknad.SøknadDAO
import no.nav.sifinnsynapi.soknad.SøknadDTO
import no.nav.sifinnsynapi.soknad.SøknadRepository
import no.nav.sifinnsynapi.utils.*
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.producer.Producer
import org.awaitility.kotlin.await
import org.junit.Assert.assertNotNull
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.context.annotation.Import
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.Duration
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit

@EmbeddedKafka( // Setter opp og tilgjengligjør embeded kafka broker
    count = 3,
    bootstrapServersProperty = "kafka-servers", // Setter bootstrap-servers for consumer og producer.
    topics = [
        PP_SYKT_BARN,
        K9_DITTNAV_VARSEL_BESKJED,
        K9_DITTNAV_VARSEL_BESKJED_AIVEN,
        AAPEN_DOK_JOURNALFØRING_V1
    ]
)
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
@ActiveProfiles("test")
@EnableMockOAuth2Server // Tilgjengliggjør en oicd-provider for test. Se application-test.yml -> no.nav.security.jwt.issuer.selvbetjening for konfigurasjon
@Import(SecurityConfiguration::class)
@AutoConfigureWireMock // Konfigurerer og setter opp en wiremockServer. Default leses src/test/resources/__files og src/test/resources/mappings
@SpringBootTest(
    classes = [SifInnsynApiApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
) // Integrasjonstest - Kjører opp hele Spring Context med alle konfigurerte beans.
class OnpremKafkaHendelseKonsumentIntegrasjonsTest {

    @Autowired
    lateinit var mapper: ObjectMapper

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker // Broker som brukes til å konfigurere opp en kafka producer.

    @Autowired
    lateinit var repository: SøknadRepository // Repository som brukes til databasekall.

    @Autowired
    lateinit var restTemplate: TestRestTemplate // Restklient som brukes til å gjøre restkall mot endepunkter i appen.

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    lateinit var mockOAuth2Server: MockOAuth2Server

    @MockkBean
    lateinit var safSelvbetjeningService: SafSelvbetjeningService

    lateinit var producer: Producer<String, Any> // Kafka producer som brukes til å legge på kafka meldinger. Mer spesifikk, Hendelser om pp-sykt-barn
    lateinit var joarkProducer: Producer<Long, JournalfoeringHendelseRecord> // Kafka producer som brukes til å legge på kafka meldinger for joark hendelser.
    lateinit var dittNavConsumer: Consumer<String, K9Beskjed> // Kafka consumer som brukes til å lese kafka meldinger.

    companion object {
        private val log: Logger =
            LoggerFactory.getLogger(OnpremKafkaHendelseKonsumentIntegrasjonsTest::class.java)
        private val aktørId = AktørId.valueOf("123456")
    }

    @BeforeAll
    fun setUp() {
        assertNotNull(mockOAuth2Server)
        producer = embeddedKafkaBroker.opprettKafkaProducer()
        joarkProducer = embeddedKafkaBroker.opprettJoarkKafkaProducer()
        dittNavConsumer = embeddedKafkaBroker.opprettDittnavConsumer()
    }

    @BeforeEach
    internal fun beforeEach() {
        repository.deleteAll()
        stubDokumentOversikt()
    }

    @AfterEach
    fun afterEach() {
        log.info("Tømmer databasen...")
        repository.deleteAll()
    }

    @AfterAll
    fun tearDown() {
        producer.close()
        joarkProducer.close()
        dittNavConsumer.close()
    }

    @Test
    fun `Gitt lagrede pleiepengesøknader i database, forvent kun at brukers data vises ved restkall`() {

        // legg på 1 hendelse om mottatt søknad om pleiepenger sykt barn...
        producer.leggPåTopic(defaultHendelse(journalpostId = "1"), PP_SYKT_BARN, mapper)

        // forvent at mottatt hendelse konsumeres og persisteres, samt at gitt restkall gitt forventet resultat.
        await.atMost(10, TimeUnit.SECONDS).untilAsserted {

            // Stub bruker aktørId, ulikt aktørId på hendelse
            stubForAktørId("annenAktørID-123456", 200)

            val responseEntity = restTemplate.exchange(
                SØKNAD,
                HttpMethod.GET,
                hentToken(),
                object : ParameterizedTypeReference<List<SøknadDTO>>() {})
            val forventetRespons =
                //language=json
                """
                       []
                    """.trimIndent()
            responseEntity.listAssert(forventetRespons, 200)
        }
    }

    @Test
    fun `Konsumere hendelse om Pleiepenger - Sykt barn, persister og tilgjengligjør gjennom API`() {

        // legg på 1 hendelse om mottatt søknad om pleiepenger sykt barn...
        producer.leggPåTopic(defaultHendelse(journalpostId = "2"), PP_SYKT_BARN, mapper)

        // forvent at mottatt hendelse konsumeres og persisteres, samt at gitt restkall gitt forventet resultat.
        await.atMost(10, TimeUnit.SECONDS).untilAsserted {
            val responseEntity = restTemplate.exchange(
                SØKNAD,
                HttpMethod.GET,
                hentToken(),
                object : ParameterizedTypeReference<List<SøknadDTO>>() {})
            val forventetRespons =
                //language=json
                """
                       [
                          {
                            "søknadstype": "PP_SYKT_BARN",
                            "status": "MOTTATT",
                            "saksId": null,
                            "journalpostId": "2",
                            "søknad": {
                              "søker": {
                                "fødselsnummer": "1234567",
                                "aktørId": "123456"
                              }
                            }
                          }
                        ]
                    """.trimIndent()
            responseEntity.listAssert(forventetRespons, 200)
        }
    }

    @Test
    fun `Konsumere hendelse om Pleiepenger - Sykt barn, hente søknad med id`() {

        // legg på 1 hendelse om mottatt søknad om pleiepenger sykt barn...
        val hendelse = defaultHendelse(journalpostId = "3")
        val søknadId = hendelse.data.melding["søknadId"] as String
        producer.leggPåTopic(hendelse, PP_SYKT_BARN, mapper)

        // forvent at mottatt hendelse konsumeres og persisteres, samt at gitt restkall gitt forventet resultat.
        await.atMost(10, TimeUnit.SECONDS).untilAsserted {
            val responseEntity =
                restTemplate.exchange("${SØKNAD}/${søknadId}", HttpMethod.GET, hentToken(), SøknadDTO::class.java)
            val forventetRespons =
                //language=json
                """
                          {
                            "søknadId": "$søknadId",
                            "søknadstype": "PP_SYKT_BARN",
                            "status": "MOTTATT",
                            "saksId": null,
                            "journalpostId": "3",
                            "søknad": {
                              "søker": {
                                "fødselsnummer": "1234567",
                                "aktørId": "123456"
                              }
                            },
                            "dokumenter": [
                              {
                                "journalpostId": "3",
                                "sakId": "1DMELD6",
                                "dokumentInfoId": "533440578",
                                "tittel": "Søknad om pleiepenger",
                                "url": "http://localhost:9999/dokument/3/533440578/ARKIV",
                                "harTilgang": true,
                                "relevanteDatoer": [
                                  {
                                    "dato": "2021-10-15T11:28:43",
                                    "datotype": "DATO_JOURNALFOERT"
                                  }
                                ]
                              }
                            ]
                          }
                    """.trimIndent()
            responseEntity.assert(forventetRespons, 200)
        }
    }

    @Test
    fun `Konsumere hendelse om Pleiepenger - Sykt barn, forevnt at dittnav beskjed blir sendt ut`() {

        // legg på 1 hendelse om mottatt søknad om pleiepenger sykt barn...
        val hendelse = defaultHendelse(journalpostId = "4")
        producer.leggPåTopic(hendelse, PP_SYKT_BARN, mapper)

        val dittnavBeskjed = dittNavConsumer.lesMelding(hendelse.data.melding["søknadId"] as String, topic = K9_DITTNAV_VARSEL_BESKJED_AIVEN)
        log.info("----> dittnav melding: {}", dittnavBeskjed)
        assertThat(dittnavBeskjed).isNotNull()
    }

    @Test
    fun `Dersom pleiepengesøknadshendelse med journalpostId og aktørId eksisterer, skip deserialisering av duplikat`() {
        val hendelse = defaultHendelse(journalpostId = "5")

        // legg på 2 hendelser som duplikater...
        producer.leggPåTopic(hendelse, PP_SYKT_BARN, mapper)
        producer.leggPåTopic(hendelse, PP_SYKT_BARN, mapper)

        // forvent at kun 1 hendelse konsumeres, og at 1 duplikat ignoreres.
        await.atMost(10, TimeUnit.SECONDS).until {
            repository.findAllByAktørId(aktørId).size == 1
        }
    }

    @Test
    fun `gitt eksisterende søknad, konsumer fra joark, slå opp journalpostinfo og oppdater søknad med saksId`() {
        val journalpostId: Long = 987654321

        repository.save(
            SøknadDAO(
                id = UUID.randomUUID(),
                opprettet = ZonedDateTime.now(),
                aktørId = AktørId(aktørId = "123456"),
                fødselsnummer = Fødselsnummer(fødselsnummer = "1234567"),
                søknadstype = Søknadstype.PP_SYKT_BARN,
                status = SøknadsStatus.MOTTATT,
                søknad = "{}",
                saksId = null,
                journalpostId = "$journalpostId"
            )
        )

        joarkProducer.leggPåTopic(defaultJournalfoeringHendelseRecord(journalpostId))

        await.atMost(Duration.ofSeconds(10)).untilAsserted {
            assertThat { repository.findByJournalpostId("$journalpostId") }.transform { it.getOrNull() }
                .isNotNull()
                .transform { it.saksId }.isNotNull()
        }
    }

    private fun ResponseEntity<List<SøknadDTO>>.listAssert(forventetResponse: String, forventetStatus: Int) {
        assertThat(statusCodeValue).isEqualTo(forventetStatus)
        JSONAssert.assertEquals(forventetResponse, body!!.somJson(mapper), JSONCompareMode.LENIENT)
    }

    private fun ResponseEntity<SøknadDTO>.assert(forventetResponse: String, forventetStatus: Int) {
        assertThat(statusCodeValue).isEqualTo(forventetStatus)
        JSONAssert.assertEquals(forventetResponse, body!!.somJson(mapper), JSONCompareMode.LENIENT)
    }

    private fun hentToken(): HttpEntity<String> = mockOAuth2Server.hentToken().tokenTilHttpEntity()

    private fun stubDokumentOversikt() {
        coEvery {
            safSelvbetjeningService.hentDokumentoversikt()
        } returns Dokumentoversikt(
            journalposter = listOf(
                Journalpost(
                    journalpostId = "3",
                    tittel = "Søknad om pleiepenger – sykt barn - NAV 09-11.05",
                    journalstatus = Journalstatus.JOURNALFOERT,
                    relevanteDatoer = listOf(
                        RelevantDato(
                            dato = "2021-10-15T11:28:43",
                            datotype = Datotype.DATO_JOURNALFOERT
                        )
                    ),
                    sak = Sak(
                        fagsakId = "1DMELD6",
                        fagsaksystem = "K9"
                    ),
                    dokumenter = listOf(
                        DokumentInfo(
                            dokumentInfoId = "533440578",
                            tittel = "Søknad om pleiepenger",
                            brevkode = "NAV 09-11.05",
                            dokumentvarianter = listOf(
                                Dokumentvariant(Variantformat.ARKIV, "PDF", true, listOf())
                            )
                        )
                    )
                ),
                Journalpost(
                    journalpostId = "4",
                    tittel = "Søknad om pleiepenger – sykt barn - NAVe 09-11.05",
                    journalstatus = Journalstatus.MOTTATT,
                    relevanteDatoer = listOf(
                        RelevantDato(
                            dato = "2021-10-15T11:28:43",
                            datotype = Datotype.DATO_OPPRETTET
                        )
                    ),
                    sak = null,
                    dokumenter = listOf(
                        DokumentInfo(
                            dokumentInfoId = "533439503",
                            tittel = "Ettersendelse pleiepenger sykt barn",
                            brevkode = "NAVe 09-11.05",
                            dokumentvarianter = listOf(
                                Dokumentvariant(Variantformat.ARKIV, "PDF", true, listOf())
                            )
                        )
                    )
                )
            )
        )
    }
}


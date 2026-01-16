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
import no.nav.sifinnsynapi.config.Issuers
import no.nav.sifinnsynapi.config.SecurityConfiguration
import no.nav.sifinnsynapi.config.Topics.AAPEN_DOK_JOURNALFØRING_V1
import no.nav.sifinnsynapi.config.Topics.K9_DITTNAV_VARSEL_BESKJED_AIVEN
import no.nav.sifinnsynapi.config.Topics.K9_ETTERSENDING
import no.nav.sifinnsynapi.config.Topics.PP_SYKT_BARN
import no.nav.sifinnsynapi.config.Topics.PP_SYKT_BARN_ENDRINGSMELDING
import no.nav.sifinnsynapi.dittnav.K9Beskjed
import no.nav.sifinnsynapi.konsument.ettersending.K9EttersendingKonsument.Companion.Ettersendelsestype
import no.nav.sifinnsynapi.konsument.pleiepenger.endringsmelding.PleiepengerEndringsmeldingDittnavBeskjedProperties
import no.nav.sifinnsynapi.safselvbetjening.SafSelvbetjeningService
import no.nav.sifinnsynapi.safselvbetjening.generated.enums.Datotype
import no.nav.sifinnsynapi.safselvbetjening.generated.enums.Journalstatus
import no.nav.sifinnsynapi.safselvbetjening.generated.enums.Variantformat
import no.nav.sifinnsynapi.safselvbetjening.generated.hentdokumentoversikt.DokumentInfo
import no.nav.sifinnsynapi.safselvbetjening.generated.hentdokumentoversikt.Dokumentoversikt
import no.nav.sifinnsynapi.safselvbetjening.generated.hentdokumentoversikt.Dokumentvariant
import no.nav.sifinnsynapi.safselvbetjening.generated.hentdokumentoversikt.Journalpost
import no.nav.sifinnsynapi.safselvbetjening.generated.hentdokumentoversikt.RelevantDato
import no.nav.sifinnsynapi.safselvbetjening.generated.hentdokumentoversikt.Sak
import no.nav.sifinnsynapi.soknad.SøknadDAO
import no.nav.sifinnsynapi.soknad.SøknadDTO
import no.nav.sifinnsynapi.soknad.SøknadRepository
import no.nav.sifinnsynapi.utils.defaultHendelse
import no.nav.sifinnsynapi.utils.defaultHendelseK9Ettersending
import no.nav.sifinnsynapi.utils.defaultHendelsePPEndringsmelding
import no.nav.sifinnsynapi.utils.defaultJournalfoeringHendelseRecord
import no.nav.sifinnsynapi.utils.hentToken
import no.nav.sifinnsynapi.utils.leggPåTopic
import no.nav.sifinnsynapi.utils.lesMelding
import no.nav.sifinnsynapi.utils.opprettDittnavConsumer
import no.nav.sifinnsynapi.utils.opprettJoarkKafkaProducer
import no.nav.sifinnsynapi.utils.opprettKafkaProducer
import no.nav.sifinnsynapi.utils.somJson
import no.nav.sifinnsynapi.utils.stubForAktørId
import no.nav.sifinnsynapi.utils.tokenTilHttpEntity
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.producer.Producer
import org.awaitility.kotlin.await
import org.json.JSONObject
import org.junit.Assert.assertNotNull
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.core.ParameterizedTypeReference
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock
import java.time.Duration
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit

@EmbeddedKafka( // Setter opp og tilgjengligjør embeded kafka broker
    count = 1,
    bootstrapServersProperty = "kafka-servers", // Setter bootstrap-servers for consumer og producer.
    topics = [
        PP_SYKT_BARN,
        K9_ETTERSENDING,
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
@EnableWireMock(ConfigureWireMock())
@AutoConfigureTestRestTemplate
@SpringBootTest(
    classes = [SifInnsynApiApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
) // Integrasjonstest - Kjører opp hele Spring Context med alle konfigurerte beans.
class KafkaHendelseKonsumentIntegrasjonsTest {

    @Autowired
    lateinit var mapper: ObjectMapper

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker // Broker som brukes til å konfigurere opp en kafka producer.

    @Autowired
    lateinit var repository: SøknadRepository // Repository som brukes til databasekall.

    @Autowired
    lateinit var restTemplate: TestRestTemplate // Restklient som brukes til å gjøre restkall mot endepunkter i appen.

    @Autowired
    lateinit var mockOAuth2Server: MockOAuth2Server

    @MockkBean
    lateinit var safSelvbetjeningService: SafSelvbetjeningService

    lateinit var producer: Producer<String, Any> // Kafka producer som brukes til å legge på kafka meldinger. Mer spesifikk, Hendelser om pp-sykt-barn
    lateinit var joarkProducer: Producer<Long, JournalfoeringHendelseRecord> // Kafka producer som brukes til å legge på kafka meldinger for joark hendelser.
    lateinit var dittNavConsumer: Consumer<String, K9Beskjed> // Kafka consumer som brukes til å lese kafka meldinger.

    @Autowired
    lateinit var endringsmeldingBeskjedProperties: PleiepengerEndringsmeldingDittnavBeskjedProperties

    companion object {
        private val log: Logger =
            LoggerFactory.getLogger(KafkaHendelseKonsumentIntegrasjonsTest::class.java)
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

        val dittnavBeskjed = dittNavConsumer.lesMelding(
            hendelse.data.melding["søknadId"] as String,
            topic = K9_DITTNAV_VARSEL_BESKJED_AIVEN
        )
        log.info("----> dittnav melding: {}", dittnavBeskjed)
        assertThat(dittnavBeskjed).isNotNull()
    }

    @Test
    fun `Konsumere hendelse om Pleiepenger - sykt barn - endringsmelding, hente søknad med id og sjekke at forventet dittnav varsel sendes`() {
        val hendelse = defaultHendelsePPEndringsmelding(journalpostId = "6")
        val søknadId = JSONObject(hendelse.data.melding).getJSONObject("k9FormatSøknad").getString("søknadId")
        producer.leggPåTopic(hendelse, PP_SYKT_BARN_ENDRINGSMELDING, mapper)

        // forvent at mottatt hendelse konsumeres og persisteres, samt at gitt restkall gir forventet resultat.
        await.atMost(15, TimeUnit.SECONDS).ignoreExceptions().untilAsserted {
            val responseEntity =
                restTemplate.exchange("${SØKNAD}/${søknadId}", HttpMethod.GET, hentToken(), SøknadDTO::class.java)
            val forventetRespons =
                //language=json
                """
                    {
                      "søknadId" : "$søknadId",
                      "søknadstype" : "PP_SYKT_BARN_ENDRINGSMELDING",
                      "status" : "MOTTATT",
                      "søknad" : {
                        "søker" : {
                          "aktørId" : "123456",
                          "fødselsnummer" : "1234567"
                        },
                        "dokumentId" : [ "123", "456" ],
                        "k9FormatSøknad" : {
                          "søknadId" : "$søknadId",
                          "mottattDato" : "2022-01-31T12:20:47.151403+01:00"
                        }
                      },
                      "saksId" : null,
                      "journalpostId" : "6",
                      "dokumenter": [
                        {
                          "journalpostId": "6",
                          "dokumentInfoId": "533440578",
                          "sakId": "1DMELD6",
                          "tittel": "Endringsmelding om pleiepenger",
                          "filtype": "PDF",
                          "harTilgang": true,
                          "url": "http://localhost:9999/dokument/6/533440578/ARKIV",
                          "relevanteDatoer": [
                            {
                              "dato": "2021-10-15T11:28:43",
                              "datotype": "DATO_JOURNALFOERT"
                            }
                          ]
                        }
                      ],
                      "behandlingsdato" : null
                    }
                """.trimIndent()
            responseEntity.assert(forventetRespons, 200)
        }

        val dittnavBeskjed = dittNavConsumer.lesMelding(søknadId, topic = K9_DITTNAV_VARSEL_BESKJED_AIVEN)
        assertThat(dittnavBeskjed).isNotNull()
        assertThat(dittnavBeskjed.toString().contains(endringsmeldingBeskjedProperties.tekst))
        assertThat(dittnavBeskjed.toString().contains(endringsmeldingBeskjedProperties.link))
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
            assertThat(runCatching<SøknadDAO?> { repository.findByJournalpostId("$journalpostId") })
                .transform { it.getOrNull() }
                .isNotNull()
                .transform { it.saksId }.isNotNull()
        }
    }

    @Test
    fun `Konsumer hendelse om ettersending av PLEIEPENGER_SYKT_BARN og forvent at dittNav beskjed blir sendt ut`() {
        val ettersendelsestype = Ettersendelsestype.PLEIEPENGER_SYKT_BARN
        val hendelse = defaultHendelseK9Ettersending(ettersendelsestype = ettersendelsestype)
        producer.leggPåTopic(hendelse, K9_ETTERSENDING, mapper)
        val søknadId = hendelse.data.melding["soknadId"] as String

        await.atMost(Duration.ofSeconds(10)).untilAsserted {
            val ettersendelse = repository.findByIdOrNull(UUID.fromString(søknadId))
            assertThat(ettersendelse).isNotNull()
            assertThat(ettersendelse!!.søknadstype).isEqualTo(Søknadstype.PP_ETTERSENDELSE)

            // forvent at dittNav melding blir sendt
            val dittnavBeskjed = dittNavConsumer.lesMelding(søknadId, K9_DITTNAV_VARSEL_BESKJED_AIVEN)
            log.info("----> dittnav melding: {}", dittnavBeskjed)
            assertThat(dittnavBeskjed).isNotNull()
            Assertions.assertTrue(dittnavBeskjed.toString().contains("pleiepenger"))
        }
    }

    @ParameterizedTest
    @EnumSource(value = Ettersendelsestype::class)
    fun `Konsumer hendelse om ettersending og forvent at dittnav beskjed blir sendt ut`(ettersendelsestype: Ettersendelsestype) {
        val hendelse = defaultHendelseK9Ettersending(ettersendelsestype = ettersendelsestype)
        producer.leggPåTopic(hendelse, K9_ETTERSENDING, mapper)
        val søknadId = hendelse.data.melding["soknadId"] as String

        await.atMost(Duration.ofSeconds(10)).untilAsserted {
            val ettersendelseDao = repository.findByIdOrNull(UUID.fromString(søknadId))
            assertThat(ettersendelseDao).isNotNull()

            when (ettersendelsestype) {
                Ettersendelsestype.PLEIEPENGER_SYKT_BARN -> assertThat(ettersendelseDao!!.søknadstype)
                    .isEqualTo(Søknadstype.PP_ETTERSENDELSE)

                Ettersendelsestype.PLEIEPENGER_LIVETS_SLUTTFASE -> assertThat(ettersendelseDao!!.søknadstype)
                    .isEqualTo(Søknadstype.PP_LIVETS_SLUTTFASE_ETTERSENDELSE)

                else -> assertThat(ettersendelseDao!!.søknadstype).isEqualTo(Søknadstype.OMS_ETTERSENDELSE)
            }

            // forvent at dittNav melding blir sendt
            val dittnavBeskjed = dittNavConsumer.lesMelding(søknadId, K9_DITTNAV_VARSEL_BESKJED_AIVEN)
            log.info("----> dittnav melding: {}", dittnavBeskjed)
            assertThat(dittnavBeskjed).isNotNull()
            when (ettersendelsestype) {
                Ettersendelsestype.PLEIEPENGER_SYKT_BARN, Ettersendelsestype.PLEIEPENGER_LIVETS_SLUTTFASE -> {
                    assertThat(dittnavBeskjed.toString().contains("pleiepenger"))
                }
                else -> {
                    assertThat(dittnavBeskjed.toString().contains("omsorgspenger"))
                }
            }
        }
    }

    private fun ResponseEntity<List<SøknadDTO>>.listAssert(forventetResponse: String, forventetStatus: Int) {
        assertThat(statusCode.value()).isEqualTo(forventetStatus)
        JSONAssert.assertEquals(forventetResponse, body!!.somJson(mapper), JSONCompareMode.LENIENT)
    }

    private fun ResponseEntity<SøknadDTO>.assert(forventetResponse: String, forventetStatus: Int) {
        assertThat(statusCode.value()).isEqualTo(forventetStatus)
        JSONAssert.assertEquals(forventetResponse, body!!.somJson(mapper), JSONCompareMode.LENIENT)
    }

    private fun hentToken(): HttpEntity<String> =
        mockOAuth2Server.hentToken(issuerId = Issuers.TOKEN_X).tokenTilHttpEntity()

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
                ),
                Journalpost(
                    journalpostId = "6",
                    tittel = "Endringsmelding om pleiepenger - NAV 09-11.05",
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
                            tittel = "Endringsmelding om pleiepenger",
                            brevkode = "NAV 09-11.05",
                            dokumentvarianter = listOf(
                                Dokumentvariant(Variantformat.ARKIV, "PDF", true, listOf())
                            )
                        )
                    )
                ),
            )
        )
    }
}


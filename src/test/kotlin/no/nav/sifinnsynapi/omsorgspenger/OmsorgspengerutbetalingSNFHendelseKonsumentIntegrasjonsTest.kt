package no.nav.sifinnsynapi.omsorgspenger

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.nimbusds.jwt.SignedJWT
import no.nav.security.token.support.test.JwtTokenGenerator
import no.nav.security.token.support.test.spring.TokenGeneratorConfiguration
import no.nav.sifinnsynapi.common.*
import no.nav.sifinnsynapi.config.Topics.OMP_UTBETALING_SNF
import no.nav.sifinnsynapi.soknad.SøknadDAO
import no.nav.sifinnsynapi.soknad.SøknadDTO
import no.nav.sifinnsynapi.soknad.SøknadRepository
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.context.annotation.Import
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit

@EmbeddedKafka( // Setter opp og tilgjengligjør embeded kafka broker
        topics = [OMP_UTBETALING_SNF],
        bootstrapServersProperty = "spring.kafka.bootstrap-servers" // Setter bootstrap-servers for consumer og producer.
)
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) // Integrasjonstest - Kjører opp hele Spring Context med alle konfigurerte beans.
@Import(TokenGeneratorConfiguration::class) // Tilgjengliggjør en oicd-provider for test. Se application-test.yml -> no.nav.security.jwt.issuer.selvbetjening for konfigurasjon
@AutoConfigureWireMock(port = 8000) // Konfigurerer og setter opp en wiremockServer. Default leses src/test/resources/__files og src/test/resources/mappings
class OmsorgspengerutbetalingSNFHendelseKonsumentIntegrasjonsTest {

    @Autowired
    @Qualifier("testMapper")
    lateinit var mapper: ObjectMapper

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker // Broker som brukes til å konfigurere opp en kafka producer.

    @Autowired
    lateinit var repository: SøknadRepository // Repository som brukes til databasekall.

    @Autowired
    lateinit var restTemplate: TestRestTemplate // Restklient som brukes til å gjøre restkall mot endepunkter i appen.

    lateinit var omsorgspengerutbetalingSnfProducer: Producer<String, Any> // Kafka producer som brukes til å legge på kafka meldinger. Mer spesifikk, Hendelser om mottatte oms-utbetaling-snf søknader.

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(OmsorgspengerutbetalingSNFHendelseKonsumentIntegrasjonsTest::class.java)
        private val aktørId = AktørId.valueOf("123456")
        private val fødselsnummer = Fødselsnummer.valueOf("1234567")

        private val httpEntity = JwtTokenGenerator.createSignedJWT(fødselsnummer.fødselsnummer).toHttpEntity()

        fun SignedJWT.toHttpEntity(): HttpEntity<String> {
            val token = serialize()
            logger.info("SignedJWT = {}", token)
            val headers = HttpHeaders()
            headers.setBearerAuth(token)
            return HttpEntity<String>(headers)
        }
    }

    @BeforeAll
    fun setUp() {
        omsorgspengerutbetalingSnfProducer = DefaultKafkaProducerFactory<String, Any>(HashMap(KafkaTestUtils.producerProps(embeddedKafkaBroker))).createProducer()
    }

    @AfterEach
    internal fun tearDown() {
        repository.deleteAll() //Tømmer databasen mellom hver test
    }

    @Test
    fun `Konsumere hendelse om Omsorgspengerutbetaling - SN&F, persister og tilgjengligjør gjennom API`() {

        // Gitt at ingen hendelser med samme aktørId eksisterer...
        repository.findAllByAktørId(aktørId).ikkeEksisterer()

        // legg på 1 hendelse om mottatt søknad om omsorgspengerutbetaling for selvstending næringsdrivende og frilans...
        omsorgspengerutbetalingSnfProducer.leggPåTopic(hendelse(), OMP_UTBETALING_SNF)

        // forvent at mottatt hendelse konsumeres og persisteres, samt at gitt restkall gitt forventet resultat.
        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            val responseEntity = restTemplate.exchange("/soknad", HttpMethod.GET, httpEntity, object : ParameterizedTypeReference<List<SøknadDTO>>() {})
            val forventetRespons =
                    //language=json
                    """
                       [
                          {
                            "søknadstype": "OMP_UTBETALING_SNF",
                            "status": "MOTTATT",
                            "saksId": null,
                            "journalpostId": "123456789",
                            "søknad": {
                              "søker": {
                                "fødselsnummer": "1234567",
                                "aktørId": "${aktørId.aktørId}"
                              }
                            }
                          }
                        ]
                    """.trimIndent()
            responseEntity.assert(forventetRespons, 200)
        }
    }

    @Test
    fun `Gitt lagrede meldinger i database, forvent kun at brukers data vises ved restkall`() {
        val hendelse1 = hendelse()

        // Gitt at ingen hendelser med samme aktørId eksisterer...
        repository.findAllByAktørId(aktørId).ikkeEksisterer()

        // legg på 1 hendelse om mottatt søknad om omsorgspengerutbetaling for selvstending næringsdrivende og frilans...
        omsorgspengerutbetalingSnfProducer.leggPåTopic(hendelse1, OMP_UTBETALING_SNF)

        // Stub bruker aktørId, ulikt aktørId på hendelse
        stubForAktørId("annenAktørID-123456", 200)

        // forvent at mottatt hendelse konsumeres og persisteres, samt at gitt restkall gitt forventet resultat.
        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            val responseEntity = restTemplate.exchange("/soknad", HttpMethod.GET, httpEntity, object : ParameterizedTypeReference<List<SøknadDTO>>() {})
            val forventetRespons =
                    //language=json
                    """
                       []
                    """.trimIndent()
            responseEntity.assert(forventetRespons, 200)
        }
    }

    private fun stubForAktørId(aktørId: String, status: Int) {
        stubFor(get(urlPathMatching("/k9-selvbetjening-oppslag-mock/meg.*"))
                .withHeader("x-nav-apiKey", matching(".*"))
                .withHeader("Authorization", matching(".*"))
                .withQueryParam("a", equalTo("aktør_id"))
                .willReturn(aResponse()
                        .withStatus(status)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                        .withBody(
                                //language=json
                                """
                            {
                                "aktør_id": "$aktørId"
                            }
                        """.trimIndent()
                        ))
        )
    }

    @Test
    fun `Dersom hendelse med journalpostId og aktørId eksisterer, skip deserialisering av duplikat`() {
        val hendelse = hendelse()

        // Gitt at ingen hendelser med samme aktørId eksisterer...
        repository.findAllByAktørId(aktørId).ikkeEksisterer()

        // legg på 2 hendelser som duplikater...
        omsorgspengerutbetalingSnfProducer.leggPåTopic(hendelse, OMP_UTBETALING_SNF)
        omsorgspengerutbetalingSnfProducer.leggPåTopic(hendelse, OMP_UTBETALING_SNF)

        // forvent at kun 1 hendelse konsumeres, og at 1 duplikat ignoreres.
        await.atMost(5, TimeUnit.SECONDS).until { repository.findAllByAktørId(aktørId).size == 1 }
    }

    private fun ResponseEntity<List<SøknadDTO>>.assert(forventetResponse: String, forventetStatus: Int) {
        assertThat(statusCodeValue).isEqualTo(forventetStatus)
        JSONAssert.assertEquals(forventetResponse, body!!.somJson(), JSONCompareMode.LENIENT)
    }

    private fun Producer<String, Any>.leggPåTopic(hendelse: TopicEntry, topic: String) {
        omsorgspengerutbetalingSnfProducer.send(ProducerRecord(topic, hendelse.somJson()))
        omsorgspengerutbetalingSnfProducer.flush()
    }

    private fun hendelse(): TopicEntry {
        return TopicEntry(
                data = OmsorgspengerutbetalingSNFHendelse(
                        metadata = metadata(),
                        melding = mapOf(
                                "soknadId" to UUID.randomUUID().toString(),
                                "mottatt" to ZonedDateTime.now(),
                                "søker" to mapOf(
                                        "fødselsnummer" to "1234567",
                                        "aktørId" to aktørId.aktørId
                                )
                        ),
                        journalførtMelding = JournalfortMelding(
                                journalpostId = "123456789"
                        )
                )
        )
    }

    private fun metadata(): Metadata {
        return Metadata(
                version = 1,
                correlationId = UUID.randomUUID().toString(),
                requestId = UUID.randomUUID().toString()
        )
    }

    private fun List<SøknadDAO>.ikkeEksisterer() {
        assertThat(this).isEmpty()
    }

    private fun List<SøknadDTO>.somJson() = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)
    private fun TopicEntry.somJson() = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)
}


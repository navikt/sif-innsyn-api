package no.nav.sifinnsynapi.omsorgspenger

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.databind.ObjectMapper
import com.nimbusds.jwt.SignedJWT
import no.nav.security.token.support.test.JwtTokenGenerator
import no.nav.security.token.support.test.spring.TokenGeneratorConfiguration
import no.nav.sifinnsynapi.common.AktørId
import no.nav.sifinnsynapi.common.JournalfortMelding
import no.nav.sifinnsynapi.common.Metadata
import no.nav.sifinnsynapi.common.TopicEntry
import no.nav.sifinnsynapi.config.Topics.OMP_UTBETALING_SNF
import no.nav.sifinnsynapi.poc.SøknadDAO
import no.nav.sifinnsynapi.poc.SøknadDTO
import no.nav.sifinnsynapi.poc.SøknadRepository
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
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.context.annotation.Import
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
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


@EmbeddedKafka(
        topics = [OMP_UTBETALING_SNF],
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TokenGeneratorConfiguration::class)
@AutoConfigureWireMock(port = 8000)
class OmsorgspengerutbetalingSNFHendelseKonsumentTest {
    @Autowired
    @Qualifier("testMapper")
    lateinit var mapper: ObjectMapper

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker

    lateinit var produsent: Producer<String, Any>

    @Autowired
    lateinit var repository: SøknadRepository

    @Autowired
    lateinit var dataSource: DataSourceProperties

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    companion object {
        private val log: Logger = LoggerFactory.getLogger(OmsorgspengerutbetalingSNFHendelseKonsumentTest::class.java)
        private val aktørId = "123456"

        private val httpEntity = JwtTokenGenerator.createSignedJWT("1234567").toHttpEntity()

        fun SignedJWT.toHttpEntity(): HttpEntity<String> {
            val token = serialize()
            log.info("SignedJWT = {}", token)
            val headers = HttpHeaders()
            headers.setBearerAuth(token)
            return HttpEntity<String>(headers)
        }
    }

    @BeforeAll
    fun setUp() {
        val configs: Map<String, Any> = HashMap(KafkaTestUtils.producerProps(embeddedKafkaBroker))
        produsent = DefaultKafkaProducerFactory<String, Any>(configs).createProducer()

        log.info("----> Datasource URL: {}", dataSource.url)
        log.info("----> Datasource USERNAME: {}", dataSource.username)
        log.info("----> Datasource PASSWORD: {}", dataSource.password)
    }

    @AfterEach
    internal fun tearDown() {
        repository.deleteAll()
    }

    @Test
    fun `Konsumere hendelse om Omsorgspengerutbetaling - SN&F, persister og tilgjengligjør gjennom API`() {
        val hendelse = hendelse()
        repository.findAllByAktørId(AktørId.valueOf(aktørId)).ikkeEksisterer()
        leggPåTopic(hendelse, OMP_UTBETALING_SNF)

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
                                "aktørId": "123456"
                              }
                            }
                          }
                        ]
                    """.trimIndent()
            responseEntity.assert(forventetRespons, 200)
        }
    }

    private fun ResponseEntity<List<SøknadDTO>>.assert(forventetResponse: String, forventetStatus: Int) {
        assertThat(statusCodeValue).isEqualTo(forventetStatus)
        JSONAssert.assertEquals(forventetResponse, body!!.somJson(), JSONCompareMode.LENIENT)
    }

    private fun leggPåTopic(hendelse: TopicEntry, topic: String) {
        produsent.send(ProducerRecord(topic, hendelse.somJson()))
        produsent.flush()
    }

    @Test
    fun `Skipper dersom duplikat`() {

        val hendelse = hendelse()
        repository.findAllByAktørId(AktørId.valueOf(aktørId)).ikkeEksisterer()
        leggPåTopic(hendelse, OMP_UTBETALING_SNF)
        await.atMost(5, TimeUnit.SECONDS).until { repository.findAllByAktørId(AktørId.valueOf(aktørId)).size == 1 }
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
                                        "aktørId" to aktørId
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


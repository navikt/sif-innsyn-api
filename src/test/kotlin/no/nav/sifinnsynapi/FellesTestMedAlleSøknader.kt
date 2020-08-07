package no.nav.sifinnsynapi

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.security.token.support.test.spring.TokenGeneratorConfiguration
import no.nav.sifinnsynapi.common.AktørId
import no.nav.sifinnsynapi.common.Fødselsnummer
import no.nav.sifinnsynapi.config.Topics
import no.nav.sifinnsynapi.config.Topics.OMP_UTBETALING_ARBEIDSTAKER
import no.nav.sifinnsynapi.config.Topics.OMP_UTBETALING_SNF
import no.nav.sifinnsynapi.config.Topics.OMP_UTVIDET_RETT
import no.nav.sifinnsynapi.config.Topics.PP_SYKT_BARN
import no.nav.sifinnsynapi.soknad.SøknadDAO
import no.nav.sifinnsynapi.soknad.SøknadDTO
import no.nav.sifinnsynapi.soknad.SøknadRepository
import no.nav.sifinnsynapi.utils.defaultHendelse
import no.nav.sifinnsynapi.utils.leggPåTopic
import no.nav.sifinnsynapi.utils.somJson
import no.nav.sifinnsynapi.utils.tokenSomHttpEntity
import org.apache.kafka.clients.producer.Producer
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.context.annotation.Import
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*
import java.util.concurrent.TimeUnit


@EmbeddedKafka( // Setter opp og tilgjengligjør embeded kafka broker
        topics = [OMP_UTBETALING_SNF, OMP_UTBETALING_ARBEIDSTAKER, OMP_UTVIDET_RETT, PP_SYKT_BARN],
        bootstrapServersProperty = "spring.kafka.bootstrap-servers" // Setter bootstrap-servers for consumer og producer.
)
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) // Integrasjonstest - Kjører opp hele Spring Context med alle konfigurerte beans.
@Import(TokenGeneratorConfiguration::class) // Tilgjengliggjør en oicd-provider for test. Se application-test.yml -> no.nav.security.jwt.issuer.selvbetjening for konfigurasjon
@AutoConfigureWireMock(port = 8000) // Konfigurerer og setter opp en wiremockServer. Default leses src/test/resources/__files og src/test/resources/mappings
class FellesTestMedAlleSøknader {
    @Autowired
    lateinit var mapper: ObjectMapper

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker // Broker som brukes til å konfigurere opp en kafka producer.

    @Autowired
    lateinit var repository: SøknadRepository // Repository som brukes til databasekall.

    @Autowired
    lateinit var restTemplate: TestRestTemplate // Restklient som brukes til å gjøre restkall mot endepunkter i appen.

    lateinit var producer: Producer<String, Any> // Kafka producer som brukes til å legge på kafka meldinger

    companion object {
        private val aktørId = AktørId.valueOf("123456")
        private val fødselsnummer = Fødselsnummer.valueOf("1234567")
        private val httpEntity = tokenSomHttpEntity(fødselsnummer)
    }

    @BeforeAll
    fun setUp() {
        producer = DefaultKafkaProducerFactory<String, Any>(HashMap(KafkaTestUtils.producerProps(embeddedKafkaBroker))).createProducer()
    }

    @AfterEach
    internal fun tearDown() {
        repository.deleteAll() //Tømmer databasen mellom hver test
    }

    @Test
    //@Ignore
    fun `Konsumerer hendelser fra alle søkander, persister og tilgjengligjør gjennom API`() {

        //Sjekker at repository er tomt
        repository.findAllByAktørId(aktørId).ikkeEksisterer()

        //Legger en hendelse om mottatt søknad om omsorgspengerutbetaling for selvstendig næringsdrivende og frilans
        producer.leggPåTopic(defaultHendelse, Topics.OMP_UTBETALING_SNF, mapper)

        //Legger en hendelse om mottatt søknad om omsorgspengerutbetaling for arbeidstaker
        producer.leggPåTopic(defaultHendelse, Topics.OMP_UTBETALING_ARBEIDSTAKER, mapper)

        //Legger en hendelse om mottatt søknad om omsorgspenger utvidet rett
        producer.leggPåTopic(defaultHendelse, Topics.OMP_UTVIDET_RETT, mapper)

        //Legger en hendelse om mottatt søknad om pleiepenger sykt barn
        producer.leggPåTopic(defaultHendelse, Topics.PP_SYKT_BARN, mapper)

        //Forventer at ved restkall mot "/soknad" så får vi alle søknadene med riktig "søknadstype" som er koblet til spesifikk aktørId
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
                          },
						  {
                            "søknadstype": "OMP_UTVIDET_RETT",
                            "status": "MOTTATT",
                            "saksId": null,
                            "journalpostId": "123456789",
                            "søknad": {
                              "søker": {
                                "fødselsnummer": "1234567",
                                "aktørId": "123456"
                              }
                            }
                          },
						  {
                            "søknadstype": "OMP_UTBETALING_ARBEIDSTAKER",
                            "status": "MOTTATT",
                            "saksId": null,
                            "journalpostId": "123456789",
                            "søknad": {
                              "søker": {
                                "fødselsnummer": "1234567",
                                "aktørId": "123456"
                              }
                            }
                          },
                          {
                            "søknadstype": "PP_SYKT_BARN",
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
        JSONAssert.assertEquals(forventetResponse, body!!.somJson(mapper), JSONCompareMode.LENIENT)
    }

    private fun List<SøknadDAO>.ikkeEksisterer() {
        assertThat(this).isEmpty()
    }

}
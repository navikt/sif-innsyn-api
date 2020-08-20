package no.nav.sifinnsynapi.omsorgspenger.utvidetrett

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.security.token.support.test.spring.TokenGeneratorConfiguration
import no.nav.sifinnsynapi.Routes.SØKNAD
import no.nav.sifinnsynapi.common.AktørId
import no.nav.sifinnsynapi.common.Fødselsnummer
import no.nav.sifinnsynapi.config.Topics.OMP_UTVIDET_RETT
import no.nav.sifinnsynapi.soknad.SøknadDAO
import no.nav.sifinnsynapi.soknad.SøknadDTO
import no.nav.sifinnsynapi.soknad.SøknadRepository
import no.nav.sifinnsynapi.utils.*
import org.apache.kafka.clients.producer.Producer
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
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.context.annotation.Import
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.concurrent.TimeUnit

@EmbeddedKafka( // Setter opp og tilgjengligjør embeded kafka broker
        topics = [OMP_UTVIDET_RETT],
        bootstrapServersProperty = "spring.kafka.bootstrap-servers" // Setter bootstrap-servers for consumer og producer.
)
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) // Integrasjonstest - Kjører opp hele Spring Context med alle konfigurerte beans.
@Import(TokenGeneratorConfiguration::class) // Tilgjengliggjør en oicd-provider for test. Se application-test.yml -> no.nav.security.jwt.issuer.selvbetjening for konfigurasjon
@AutoConfigureWireMock(port = 8000) // Konfigurerer og setter opp en wiremockServer. Default leses src/test/resources/__files og src/test/resources/mappings
class OmsorgspengerUtvidetRettHendelseKonsumentIntegrasjonsTest {

    @Autowired
    lateinit var mapper: ObjectMapper

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker // Broker som brukes til å konfigurere opp en kafka producer.

    @Autowired
    lateinit var repository: SøknadRepository // Repository som brukes til databasekall.

    @Autowired
    lateinit var restTemplate: TestRestTemplate // Restklient som brukes til å gjøre restkall mot endepunkter i appen.

    lateinit var omsorgspengerUtvidetRettProducer: Producer<String, Any> // Kafka producer som brukes til å legge på kafka meldinger. Mer spesifikk, Hendelser om mottatte oms-utbetaling-snf søknader.

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(OmsorgspengerUtvidetRettHendelseKonsumentIntegrasjonsTest::class.java)
        private val aktørId = AktørId.valueOf("123456")
        private val fødselsnummer = Fødselsnummer.valueOf("1234567")
        private val httpEntity = tokenSomHttpEntity(fødselsnummer)
    }

    @BeforeAll
    fun setUp() {
        omsorgspengerUtvidetRettProducer = embeddedKafkaBroker.createKafkaProducer()
    }

    @AfterEach
    internal fun tearDown() {
        repository.deleteAll() //Tømmer databasen mellom hver test
    }

    @Test
    fun `Konsumere hendelse om Omsorgspenger - Utvidet rett, persister og tilgjengligjør gjennom API`() {

        // Gitt at ingen hendelser med samme aktørId eksisterer...
        repository.findAllByAktørId(aktørId).ikkeEksisterer()

        // legg på 1 hendelse om mottatt søknad om omsorgspenger utvidet rett...
        omsorgspengerUtvidetRettProducer.leggPåTopic(defaultHendelse, OMP_UTVIDET_RETT, mapper)

        // forvent at mottatt hendelse konsumeres og persisteres, samt at gitt restkall gitt forventet resultat.
        await.atMost(60, TimeUnit.SECONDS).untilAsserted {
            val responseEntity = restTemplate.exchange(SØKNAD, HttpMethod.GET, httpEntity, object : ParameterizedTypeReference<List<SøknadDTO>>() {})
            val forventetRespons =
                    //language=json
                    """
                       [
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
                          }
                        ]
                    """.trimIndent()
            responseEntity.assert(forventetRespons, 200)
        }
    }

    @Test
    fun `Gitt lagrede meldinger i database, forvent kun at brukers data vises ved restkall`() {
        // Gitt at ingen hendelser med samme aktørId eksisterer...
        repository.findAllByAktørId(aktørId).ikkeEksisterer()

        // legg på 1 hendelse om mottatt søknad om omsorgspenger utvidet rett...
        omsorgspengerUtvidetRettProducer.leggPåTopic(defaultHendelse, OMP_UTVIDET_RETT, mapper)

        // Stub bruker aktørId, ulikt aktørId på hendelse
        stubForAktørId("annenAktørID-123456", 200)

        // forvent at mottatt hendelse konsumeres og persisteres, samt at gitt restkall gitt forventet resultat.
        await.atMost(60, TimeUnit.SECONDS).untilAsserted {
            val responseEntity = restTemplate.exchange(SØKNAD, HttpMethod.GET, httpEntity, object : ParameterizedTypeReference<List<SøknadDTO>>() {})
            val forventetRespons =
                    //language=json
                    """
                       []
                    """.trimIndent()
            responseEntity.assert(forventetRespons, 200)
        }
    }

    @Test
    fun `Dersom hendelse med journalpostId og aktørId eksisterer, skip deserialisering av duplikat`() {
        val hendelse = defaultHendelse

        // Gitt at ingen hendelser med samme aktørId eksisterer...
        repository.findAllByAktørId(aktørId).ikkeEksisterer()

        // legg på 2 hendelser som duplikater...
        omsorgspengerUtvidetRettProducer.leggPåTopic(hendelse, OMP_UTVIDET_RETT, mapper)
        omsorgspengerUtvidetRettProducer.leggPåTopic(hendelse, OMP_UTVIDET_RETT, mapper)

        // forvent at kun 1 hendelse konsumeres, og at 1 duplikat ignoreres.
        await.atMost(60, TimeUnit.SECONDS).until { repository.findAllByAktørId(aktørId).size == 1 }
    }

    private fun ResponseEntity<List<SøknadDTO>>.assert(forventetResponse: String, forventetStatus: Int) {
        assertThat(statusCodeValue).isEqualTo(forventetStatus)
        JSONAssert.assertEquals(forventetResponse, body!!.somJson(mapper), JSONCompareMode.LENIENT)
    }

    private fun List<SøknadDAO>.ikkeEksisterer() {
        assertThat(this).isEmpty()
    }
}

package no.nav.sifinnsynapi.konsument

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.sifinnsynapi.common.AktørId
import no.nav.sifinnsynapi.config.Topics.K9_DITTNAV_VARSEL_BESKJED
import no.nav.sifinnsynapi.config.Topics.PP_SYKT_BARN
import no.nav.sifinnsynapi.dittnav.DittnavService
import no.nav.sifinnsynapi.soknad.SøknadRepository
import no.nav.sifinnsynapi.utils.defaultHendelse
import no.nav.sifinnsynapi.utils.leggPåTopic
import no.nav.sifinnsynapi.utils.opprettKafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*
import java.util.concurrent.TimeUnit

@EmbeddedKafka( // Setter opp og tilgjengligjør embeded kafka broker
    count = 3,
    topics = [PP_SYKT_BARN, K9_DITTNAV_VARSEL_BESKJED],
    bootstrapServersProperty = "kafka.onprem.servers" // Setter bootstrap-servers for consumer og producer.
)
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) // Integrasjonstest - Kjører opp hele Spring Context med alle konfigurerte beans.
@EnableMockOAuth2Server // Tilgjengliggjør en oicd-provider for test. Se application-test.yml -> no.nav.security.jwt.issuer.selvbetjening for konfigurasjon
@AutoConfigureWireMock // Konfigurerer og setter opp en wiremockServer. Default leses src/test/resources/__files og src/test/resources/mappings
class KafkaTransactionRollbackTest {

    @Autowired
    lateinit var mapper: ObjectMapper

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker // Broker som brukes til å konfigurere opp en kafka producer.

    @Autowired
    lateinit var repository: SøknadRepository // Repository som brukes til databasekall.

    @MockkBean()
    lateinit var dittnavService: DittnavService

    lateinit var producer: Producer<String, Any> // Kafka producer som brukes til å legge på kafka meldinger. Mer spesifikk, Hendelser om pp-sykt-barn

    companion object {
        private val aktørId = AktørId.valueOf("123456")
    }

    @BeforeAll
    fun setUp() {
        producer = embeddedKafkaBroker.opprettKafkaProducer()
    }

    @AfterEach
    internal fun tearDown() {
        repository.deleteAll() //Tømmer databasen mellom hver test
    }

    @Test
    fun `Konsumere hendelse, forevnt rolback ved feil`() {
        every {
            dittnavService.sendBeskjedOnprem(any(), any())
        } throws Exception("Ooops, noe gikk galt...")

        // legg på 1 hendelse om mottatt søknad om pleiepenger sykt barn...
        val hendelse = defaultHendelse(journalpostId = "0000000")
        producer.leggPåTopic(hendelse, PP_SYKT_BARN, mapper)

        // forvent at det som ble persistert blir rullet tilbake.
        await.atMost(60, TimeUnit.SECONDS).untilAsserted {
            assertThat(repository.findById(UUID.fromString(hendelse.data.melding["søknadId"] as String)))
                .isEqualTo(Optional.empty())
        }
    }
}

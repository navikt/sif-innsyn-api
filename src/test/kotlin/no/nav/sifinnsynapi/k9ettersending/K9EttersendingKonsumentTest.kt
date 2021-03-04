package no.nav.sifinnsynapi.k9ettersending

import assertk.assertThat
import assertk.assertions.isNotEmpty
import com.fasterxml.jackson.databind.ObjectMapper
import junit.framework.Assert.assertTrue
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.sifinnsynapi.config.Topics
import no.nav.sifinnsynapi.dittnav.K9Beskjed
import no.nav.sifinnsynapi.utils.*
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.producer.Producer
import org.awaitility.kotlin.await
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.concurrent.TimeUnit

@EmbeddedKafka( // Setter opp og tilgjengligjør embeded kafka broker
        count = 3,
        topics = [Topics.K9_ETTERSENDING, Topics.K9_DITTNAV_VARSEL_BESKJED],
        bootstrapServersProperty = "spring.kafka.bootstrap-servers" // Setter bootstrap-servers for consumer og producer.
)
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) // Integrasjonstest - Kjører opp hele Spring Context med alle konfigurerte beans.
@EnableMockOAuth2Server // Tilgjengliggjør en oicd-provider for test. Se application-test.yml -> no.nav.security.jwt.issuer.selvbetjening for konfigurasjon
@AutoConfigureWireMock // Konfigurerer og setter opp en wiremockServer. Default leses src/test/resources/__files og src/test/resources/mappings
class K9EttersendingKonsumentTest {

    @Autowired
    lateinit var mapper: ObjectMapper

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker // Broker som brukes til å konfigurere opp en kafka producer.

    lateinit var producer: Producer<String, Any> // Kafka producer som brukes til å legge på kafka meldinger. Mer spesifikk, Hendelser om pp-sykt-barn
    lateinit var dittNavConsumer: Consumer<String, K9Beskjed> // Kafka consumer som brukes til å lese kafka meldinger.

    companion object {
        private val log: Logger = LoggerFactory.getLogger(K9EttersendingKonsumentTest::class.java)
    }

    @BeforeAll
    fun setUp() {
        producer = embeddedKafkaBroker.opprettKafkaProducer()
        dittNavConsumer = embeddedKafkaBroker.opprettDittnavConsumer()
    }

    @Test
    fun `Konsumer hendelse om ettersending av pleiepenger og forvent at dittNav beskjed blir sendt ut`(){
        val søknadstype: String = "pleiepenger"
        val hendelse = defaultHendelseK9Ettersending(søknadstype = søknadstype)
        producer.leggPåTopic(hendelse, Topics.K9_ETTERSENDING, mapper)

        // forvent at dittNav melding blir sendt
        await.atMost(60, TimeUnit.SECONDS).untilAsserted {
            val lesMelding = dittNavConsumer.lesMelding(hendelse.data.melding["soknadId"] as String)
            log.info("----> dittnav melding: {}", lesMelding)
            assertThat(lesMelding).isNotEmpty()
            assertTrue(lesMelding.toString().contains(søknadstype))
        }
    }

    @Test
    fun `Konsumer hendelse om ettersending av omsorgspenger og forvent at dittNav beskjed blir sendt ut`(){
        val søknadstype: String = "omsorgspenger"
        val hendelse = defaultHendelseK9Ettersending(søknadstype = søknadstype)
        producer.leggPåTopic(hendelse, Topics.K9_ETTERSENDING, mapper)

        // forvent at dittNav melding blir sendt
        await.atMost(60, TimeUnit.SECONDS).untilAsserted {
            val lesMelding = dittNavConsumer.lesMelding(hendelse.data.melding["soknadId"] as String)
            log.info("----> dittnav melding: {}", lesMelding)
            assertThat(lesMelding).isNotEmpty()
            assertTrue(lesMelding.toString().contains(søknadstype))
        }
    }


}

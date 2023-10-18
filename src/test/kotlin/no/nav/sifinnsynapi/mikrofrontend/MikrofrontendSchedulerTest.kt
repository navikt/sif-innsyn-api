package no.nav.sifinnsynapi.mikrofrontend

import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.sifinnsynapi.config.Topics
import no.nav.sifinnsynapi.dittnav.MicrofrontendAction
import no.nav.sifinnsynapi.dittnav.MicrofrontendId
import no.nav.sifinnsynapi.utils.entriesOnTopic
import no.nav.sifinnsynapi.utils.opprettKafkaStringConsumer
import no.nav.sifinnsynapi.utils.stubForLeaderElection
import org.apache.kafka.clients.consumer.Consumer
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.Duration
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit


@EmbeddedKafka(
    // Setter opp og tilgjengligjør embeded kafka broker
    count = 3,
    bootstrapServersProperty = "kafka-servers", // Setter bootstrap-servers for consumer og producer.
    topics = [
        Topics.K9_DITTNAV_VARSEL_MICROFRONTEND,
    ],
    partitions = 1,
)
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
@ActiveProfiles("test")
@EnableMockOAuth2Server // Tilgjengliggjør en oicd-provider for test.
@AutoConfigureWireMock
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) // Integrasjonstest - Kjører opp hele Spring Context med alle konfigurerte beans.
class MikrofrontendSchedulerTest {

    @Autowired
    private lateinit var mikrofrontendScheduler: MikrofrontendScheduler

    @Autowired
    lateinit var mikrofrontendRepository: MikrofrontendRepository

    @Autowired
    private lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker

    lateinit var dittnavStringConsumer: Consumer<String, String> // Kafka consumer som brukes til å lese meldinger.

    private companion object {
        private val logger = LoggerFactory.getLogger(MikrofrontendSchedulerTest::class.java)
        private const val ANTALL_MELDINGER = 2_000
    }

    @BeforeAll
    fun setUp() {
        dittnavStringConsumer =
            embeddedKafkaBroker.opprettKafkaStringConsumer(
                groupId = "dittnav-consumer",
                topics = listOf(Topics.K9_DITTNAV_VARSEL_MICROFRONTEND)
            )

        stubForLeaderElection()

        logger.info("Populerer databasen med mikrofrontend entiteter")
        // indexed for-loop is faster than IntStream.range
        val entities = (0 until ANTALL_MELDINGER).map {
            MikrofrontendDAO(
                id = UUID.randomUUID(),
                fødselsnummer = "$it",
                mikrofrontendId = MicrofrontendId.PLEIEPENGER_INNSYN.id,
                status = MicrofrontendAction.ENABLE,
                opprettet = ZonedDateTime.now(),
                endret = null,
                behandlingsdato = null,
            )
        }

        mikrofrontendRepository.saveAll(entities)
        logger.info("Populering av databasen med mikrofrontend entiteter ferdig")
    }

    @AfterAll
    internal fun tearDown() {
        dittnavStringConsumer.close()
    }

    @Test
    fun `deaktiverAlleDinePleiepengerMicrofrontend`() {
        assertDoesNotThrow { mikrofrontendScheduler.deaktiverAlleDinePleiepengerMicrofrontend() }
        await().atMost(10, TimeUnit.SECONDS).untilAsserted {
            val entries = dittnavStringConsumer.entriesOnTopic(
                Topics.K9_DITTNAV_VARSEL_MICROFRONTEND,
                Duration.ofMinutes(2)
            )
            Assertions.assertEquals(ANTALL_MELDINGER, entries.count())
        }
    }
}

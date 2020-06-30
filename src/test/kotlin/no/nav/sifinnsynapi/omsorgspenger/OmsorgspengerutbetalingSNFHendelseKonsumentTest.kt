package no.nav.sifinnsynapi.omsorgspenger

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.sifinnsynapi.common.JournalfortMelding
import no.nav.sifinnsynapi.common.Metadata
import no.nav.sifinnsynapi.config.Topics
import no.nav.sifinnsynapi.config.Topics.OMP_UTBETALING_SNF
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.ZonedDateTime
import java.util.*

@EmbeddedKafka(
        topics = [Topics.OMP_UTBETALING_SNF, Topics.INNSYN_MOTTATT],
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
@ActiveProfiles("test")
@SpringBootTest
class OmsorgspengerutbetalingSNFHendelseKonsumentTest {
    @Autowired
    lateinit var mapper: ObjectMapper

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker

    lateinit var produsent: Producer<String, Any>

    companion object {
        private val log: Logger = LoggerFactory.getLogger(OmsorgspengerutbetalingSNFHendelseKonsumentTest::class.java)
    }

    @BeforeAll
    fun setUp() {
        val configs: Map<String, Any> = HashMap(KafkaTestUtils.producerProps(embeddedKafkaBroker))
        produsent = DefaultKafkaProducerFactory<String, Any>(configs).createProducer()
    }

    @Test
    fun `Konsumere hendelse fra oms utbetaling SNF`() {
        val hendelse = OmsorgspengerutbetalingSNFHendelse(
                metadata = Metadata(
                        version = 1,
                        correlationId = UUID.randomUUID().toString(),
                        requestId = UUID.randomUUID().toString()
                ),
                melding = mapOf(
                        "soknadId" to UUID.randomUUID().toString(),
                        "mottatt" to ZonedDateTime.now(),
                        "søker" to mapOf(
                                "fødselsnummer" to "1234567",
                                "aktørId" to "123456744"
                        )
                ),
                journalførtMelding = JournalfortMelding(
                        journalpostId = "123456789"
                )
        )

        val jsonString = mapper.writeValueAsString(hendelse)
        log.info("hendelse som jsonString: {}", jsonString)

        val pojo = mapper.readValue(jsonString, OmsorgspengerutbetalingSNFHendelse::class.java)
        log.info("hendelse tilbake til POJO: {}", pojo)

        produsent.send(ProducerRecord(OMP_UTBETALING_SNF, jsonString))
        produsent.flush()
    }
}
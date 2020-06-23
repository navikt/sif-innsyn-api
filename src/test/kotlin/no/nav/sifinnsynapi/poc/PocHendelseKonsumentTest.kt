package no.nav.sifinnsynapi.poc

import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import no.nav.sifinnsynapi.config.Topics.INNSYN_MOTTATT
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension


@EmbeddedKafka(
        topics = [INNSYN_MOTTATT],
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
@ActiveProfiles("test")
@SpringBootTest
internal class PocHendelseKonsumentTest {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(PocHendelseKonsumentTest::class.java)
    }

    @Autowired
    private lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker

    @InjectMockKs
    private lateinit var pocHendelseKonsumentTest: PocHendelseKonsumentTest

    @MockK
    private lateinit var søknadRepositoryMock: SøknadRepository


    @Test
    internal fun name() {
        // Arrange
        val configs: Map<String, Any> = HashMap(KafkaTestUtils.producerProps(embeddedKafkaBroker))
        val producer: Producer<String, String> = DefaultKafkaProducerFactory(configs, StringSerializer(), StringSerializer()).createProducer()

        // Act
        producer.send(ProducerRecord(INNSYN_MOTTATT,  "Hellow World!!"))
        producer.flush()

    }
}

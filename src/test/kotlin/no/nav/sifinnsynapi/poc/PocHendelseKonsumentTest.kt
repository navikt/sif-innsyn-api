package no.nav.sifinnsynapi.poc

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.verify
import no.nav.sifinnsynapi.common.AktørId
import no.nav.sifinnsynapi.common.Fødselsnummer
import no.nav.sifinnsynapi.common.SøknadsStatus
import no.nav.sifinnsynapi.common.Søknadstype
import no.nav.sifinnsynapi.config.Topics.INNSYN_MOTTATT
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.Ignore
import org.junit.jupiter.api.BeforeAll
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
import java.time.ZonedDateTime


@EmbeddedKafka(
        topics = [INNSYN_MOTTATT],
        bootstrapServersProperty = "spring.kafka.consumer.bootstrap-servers"
)
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext
@ActiveProfiles("test")
@SpringBootTest
class PocHendelseKonsumentTest {

    @Autowired
    lateinit var mapper: ObjectMapper

    companion object {
        private val log: Logger = LoggerFactory.getLogger(PocHendelseKonsumentTest::class.java)
    }

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker

    @Autowired
    private lateinit var pocHendelseKonsument: PocHendelseKonsument

    @MockkBean(relaxed = true, relaxUnitFun = true)
    private lateinit var søknadRepository: SøknadRepository

    lateinit var produsent: Producer<String, String>

    @BeforeAll
    fun setUp() {

        val configs: Map<String, Any> = HashMap(KafkaTestUtils.producerProps(embeddedKafkaBroker))
        produsent = DefaultKafkaProducerFactory(configs, StringSerializer(), StringSerializer()).createProducer()
    }

    @Ignore
    @Test
    fun `Prosesser og lagre melding`() {

        val hendelse = SøknadsHendelse(
                aktørId = AktørId.valueOf("1234567"),
                fødselsnummer = Fødselsnummer.valueOf("987654321"),
                journalpostId = "23432444",
                saksnummer = null,
                status = SøknadsStatus.MOTTATT,
                søknadstype = Søknadstype.PP_SYKT_BARN,
                førsteBehandlingsdato = null,
                mottattDato = ZonedDateTime.now(),
                søknad = mapOf()
        )

        val jsonString = mapper.writeValueAsString(hendelse)
        log.info("hendelse som jsonString: {}", jsonString)

        val pojo = mapper.readValue(jsonString, SøknadsHendelse::class.java)
        log.info("hendelse tilbake til POJO: {}", pojo)

        produsent.send(ProducerRecord(INNSYN_MOTTATT, jsonString))
        produsent.flush()

        verify(exactly = 1) {
            val any = any<SøknadDAO>()
            søknadRepository.save(any)
        }
    }
}

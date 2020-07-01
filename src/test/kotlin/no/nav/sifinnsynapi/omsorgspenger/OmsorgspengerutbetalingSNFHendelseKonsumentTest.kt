package no.nav.sifinnsynapi.omsorgspenger

import assertk.assertThat
import assertk.assertions.isEmpty
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.sifinnsynapi.common.AktørId
import no.nav.sifinnsynapi.common.JournalfortMelding
import no.nav.sifinnsynapi.common.Metadata
import no.nav.sifinnsynapi.common.TopicEntry
import no.nav.sifinnsynapi.config.Topics.OMP_UTBETALING_SNF
import no.nav.sifinnsynapi.poc.SøknadDAO
import no.nav.sifinnsynapi.poc.SøknadRepository
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.kotlin.await
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.test.context.SpringBootTest
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

    companion object {
        private val log: Logger = LoggerFactory.getLogger(OmsorgspengerutbetalingSNFHendelseKonsumentTest::class.java)
        private val aktørId = "123456744"
    }

    @BeforeAll
    fun setUp() {
        val configs: Map<String, Any> = HashMap(KafkaTestUtils.producerProps(embeddedKafkaBroker))
        produsent = DefaultKafkaProducerFactory<String, Any>(configs).createProducer()

        log.info("----> Datasource URL: {}", dataSource.url)
        log.info("----> Datasource USERNAME: {}", dataSource.username)
        log.info("----> Datasource PASSWORD: {}", dataSource.password)
    }

    @Test
    fun `Konsumere hendelse fra oms utbetaling SNF`() {
        val hendelse = TopicEntry(
                data = OmsorgspengerutbetalingSNFHendelse(
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
                                        "aktørId" to aktørId
                                )
                        ),
                        journalførtMelding = JournalfortMelding(
                                journalpostId = "123456789"
                        )
                )
        )

        val jsonString = mapper.writeValueAsString(hendelse)
        log.info("hendelse som jsonString: {}", jsonString)

        val pojo = mapper.readValue(jsonString, TopicEntry::class.java)
        log.info("hendelse tilbake til POJO: {}", pojo)

        val søknaderFørProsessering: List<SøknadDAO> = repository.findAllByAktørId(AktørId.valueOf(aktørId))
        assertThat(søknaderFørProsessering).isEmpty()

        produsent.send(ProducerRecord(OMP_UTBETALING_SNF, jsonString))
        produsent.flush()

        await.atMost(5, TimeUnit.SECONDS).until { repository.findAllByAktørId(AktørId.valueOf(aktørId)).isNotEmpty() }
    }

    @Test
    fun `Skipper dersom duplikat`() {
        val hendelse = TopicEntry(
                data = OmsorgspengerutbetalingSNFHendelse(
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
                                        "aktørId" to aktørId
                                )
                        ),
                        journalførtMelding = JournalfortMelding(
                                journalpostId = "123456789"
                        )
                )
        )

        val jsonString = mapper.writeValueAsString(hendelse)
        log.info("hendelse som jsonString: {}", jsonString)

        val pojo = mapper.readValue(jsonString, TopicEntry::class.java)
        log.info("hendelse tilbake til POJO: {}", pojo)

        val søknaderFørProsessering: List<SøknadDAO> = repository.findAllByAktørId(AktørId.valueOf(aktørId))
        assertThat(søknaderFørProsessering).isEmpty()

        produsent.send(ProducerRecord(OMP_UTBETALING_SNF, jsonString))
        produsent.flush()

        produsent.send(ProducerRecord(OMP_UTBETALING_SNF, jsonString))
        produsent.flush()

        await.atMost(5, TimeUnit.SECONDS).until { repository.findAllByAktørId(AktørId.valueOf(aktørId)).size == 1 }
    }


}


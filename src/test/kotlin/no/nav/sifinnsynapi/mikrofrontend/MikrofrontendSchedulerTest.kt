package no.nav.sifinnsynapi.mikrofrontend

import assertk.assertThat
import assertk.assertions.isEqualTo
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.sifinnsynapi.common.AktørId
import no.nav.sifinnsynapi.common.Fødselsnummer
import no.nav.sifinnsynapi.common.SøknadsStatus
import no.nav.sifinnsynapi.common.Søknadstype
import no.nav.sifinnsynapi.config.Topics
import no.nav.sifinnsynapi.dittnav.MicrofrontendAction
import no.nav.sifinnsynapi.dittnav.MicrofrontendId
import no.nav.sifinnsynapi.soknad.SøknadDAO
import no.nav.sifinnsynapi.soknad.SøknadRepository
import no.nav.sifinnsynapi.utils.opprettKafkaStringConsumer
import no.nav.sifinnsynapi.utils.stubForLeaderElection
import org.apache.kafka.clients.consumer.Consumer
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock
import java.time.Duration
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit


@EmbeddedKafka(
    // Setter opp og tilgjengligjør embeded kafka broker
    count = 1,
    bootstrapServersProperty = "kafka-servers", // Setter bootstrap-servers for consumer og producer.
    topics = [
        Topics.K9_DITTNAV_VARSEL_MICROFRONTEND,
    ],
    partitions = 1,
    controlledShutdown = true
)
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
@EnableMockOAuth2Server // Tilgjengliggjør en oicd-provider for test.
@EnableWireMock(ConfigureWireMock())
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) // Integrasjonstest - Kjører opp hele Spring Context med alle konfigurerte beans.
class MikrofrontendSchedulerTest {

    @Autowired
    private lateinit var mikrofrontendScheduler: MikrofrontendScheduler

    @Autowired
    lateinit var mikrofrontendRepository: MikrofrontendRepository

    @Autowired
    lateinit var søknadRepository: SøknadRepository

    @Autowired
    private lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker

    lateinit var dittnavStringConsumer: Consumer<String, String> // Kafka consumer som brukes til å lese meldinger.

    private companion object {
        private val logger = LoggerFactory.getLogger(MikrofrontendSchedulerTest::class.java)
        private const val ANTALL_MELDINGER = 10
    }

    @BeforeEach
    fun setUp() {
        dittnavStringConsumer =
            embeddedKafkaBroker.opprettKafkaStringConsumer(
                groupId = "dittnav-consumer-${UUID.randomUUID()}",
                topics = listOf(Topics.K9_DITTNAV_VARSEL_MICROFRONTEND)
            )
    }

    @AfterEach
    internal fun tearDown() {
        dittnavStringConsumer.close()
    }

    @Test
    @Disabled("Feiler nå alle tester kjører samtidig, men ikke alene")
    fun `Deaktiver alle dine-pleiepenger mikrofrontend`() {
        stubForLeaderElection()
        populerDatabase()

        assertDoesNotThrow { mikrofrontendScheduler.deaktiverAlleDinePleiepengerMicrofrontend() }
        await().atMost(20, TimeUnit.SECONDS).untilAsserted {
            val records = KafkaTestUtils.getRecords(dittnavStringConsumer, Duration.ofMinutes(3))
            Assertions.assertEquals(ANTALL_MELDINGER, records.count())
        }

        tømDatabase()
    }

    @Test
    @Disabled("Feiler nå alle tester kjører samtidig, men ikke alene")
    fun `aktiver dine-pleiepenger mikrofrontend siste 6 mnd`() {
        stubForLeaderElection()
        val modulus = 2
        populerDatabase(modulus)

        assertDoesNotThrow { mikrofrontendScheduler.aktiverMikrofrontendForPleiepengesøknaderDeSisteSeksMåneder() }

        await().atMost(20, TimeUnit.SECONDS).untilAsserted {
            val records = KafkaTestUtils.getRecords(dittnavStringConsumer, Duration.ofMinutes(2))
            Assertions.assertEquals(ANTALL_MELDINGER / modulus, records.count())
        }

        assertThat(mikrofrontendRepository.findAll().size).isEqualTo(ANTALL_MELDINGER)

        tømDatabase()
    }

    /**
     * Metoden populerDatabase brukes for å fylle databasen med mikrofrontend-entiteter. Antallet entiteter som opprettes og lagres,
     * styres av parameteren 'modulus'. Denne funksjonen itererer gjennom et fast antall meldinger, bestemt av ANTALL_MELDINGER, og
     * oppretter entiteter for hver iterasjon. Imidlertid blir spesielle handlinger anvendt på hver 'modulus'-te melding.
     *
     * @param modulus en integer-verdi som bestemmer frekvensen av spesielle handlinger på entitetene. For eksempel, hvis modulus er 3,
     *                vil spesielle handlinger bli brukt på hver tredje entitet. En spesiell handling kan være å endre opprettelsesdatoen
     *                til entiteten eller å lagre entiteten i en annen repository. Hvis 'it' (indeksen i løkken) er et multiplum av 'modulus',
     *                blir opprettelsesdatoen satt til 6 måneder og en dag tilbake i tid, og mikrofrontend-entiteten blir lagret i mikrofrontendRepository.
     *                For andre verdier av 'it' settes opprettelsesdatoen til dagens dato, og entiteten lagres kun i søknadRepository.
     */
    private fun populerDatabase(modulus: Int? = null) {
        logger.info("Populerer databasen med mikrofrontend entiteter")
        // indexed for-loop is faster than IntStream.range
        (0 until ANTALL_MELDINGER).forEach {
            val opprettet = when {
                modulus == null -> ZonedDateTime.now()
                it % modulus == 0 -> { //
                    ZonedDateTime.now().minusMonths(6).minusDays(1)
                }
                else -> {
                    ZonedDateTime.now()
                }
            }

            val søknadDAO = SøknadDAO(
                id = UUID.randomUUID(),
                aktørId = AktørId("$it"),
                fødselsnummer = Fødselsnummer("$it"),
                søknadstype = Søknadstype.PP_SYKT_BARN,
                status = SøknadsStatus.MOTTATT,
                søknad = """{}""",
                saksId = null,
                journalpostId = "$it",
                opprettet = opprettet,
                endret = null,
                behandlingsdato = null
            )
            val mikrofrontendDAO = MikrofrontendDAO(
                id = UUID.randomUUID(),
                fødselsnummer = "$it",
                mikrofrontendId = MicrofrontendId.PLEIEPENGER_INNSYN.id,
                status = MicrofrontendAction.ENABLE,
                opprettet = opprettet,
                endret = null,
                behandlingsdato = null,
            )
            søknadRepository.save(søknadDAO)

            if (modulus == null) {
                mikrofrontendRepository.save(mikrofrontendDAO)
            } else if (it % modulus == 0) {
                mikrofrontendRepository.save(mikrofrontendDAO)
            }
        }
        logger.info("Populering av databasen med mikrofrontend entiteter ferdig")
    }

    private fun tømDatabase() {
        logger.info("Tømmer databasen...")
        mikrofrontendRepository.deleteAll()
        søknadRepository.deleteAll()
        logger.info("Tømming av databasen ferdig")
    }
}

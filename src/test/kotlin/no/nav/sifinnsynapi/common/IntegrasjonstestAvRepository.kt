package no.nav.sifinnsynapi.common

import no.nav.security.token.support.test.spring.TokenGeneratorConfiguration
import no.nav.sifinnsynapi.poc.SøknadDAO
import no.nav.sifinnsynapi.poc.SøknadRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) // Integrasjonstest - Kjører opp hele Spring Context med alle konfigurerte beans.
@Import(TokenGeneratorConfiguration::class) // Tilgjengliggjør en oicd-provider for test. Se application-test.yml -> no.nav.security.jwt.issuer.selvbetjening for konfigurasjon
class IntegrasjonstestAvRepository {

    @Autowired
    lateinit var repository: SøknadRepository // Repository som brukes til databasekall.

    companion object {
        private val aktørId = AktørId.valueOf("123456")
        private val aktørIdSomIkkeEksisterer = AktørId.valueOf("54321")
        private val fødselsnummer = Fødselsnummer.valueOf("1234567")
        private val journalpostId = "12345"
    }

    @AfterEach
    internal fun tearDown() {
        repository.deleteAll() //Tømmer databasen mellom hver test
    }

    @Test
    fun `Lagrer søknad i repository og henter opp basert på journalpostId`() {
        val søknadDAO = lagSøknadDAO()

        repository.save(søknadDAO)

        val søknadHentetFraRepository = repository.findByJournalpostId(journalpostId)

        assert(søknadHentetFraRepository != null && søknadHentetFraRepository.journalpostId == journalpostId && søknadHentetFraRepository.aktørId == aktørId)
    }

    @Test
     fun `Prøver å hente opp søknad som ikke eksisterer basert på journalpostId`() {
        val journalpostIdSomIkkeEksisterer = "54321"

        val søknadDAO = lagSøknadDAO()

        repository.save(søknadDAO)

        val søknadHentetFraRepository = repository.findByJournalpostId(journalpostIdSomIkkeEksisterer)

        assert(søknadHentetFraRepository == null)
    }

    @Test
    fun `Lagre to søknader med samme aktørId i repository og finne de basert på aktørId`(){
        var søknadDAO = lagSøknadDAO()
        repository.save(søknadDAO)

        søknadDAO = lagSøknadDAO()
        repository.save(søknadDAO)

        val søknaderHentetFraRepository = repository.findAllByAktørId(aktørId)

        assert(søknaderHentetFraRepository.size == 2)
    }

    @Test
    fun `Hente søknader som ikke finnes basert på aktørId`(){
        var søknadDAO = lagSøknadDAO()
        repository.save(søknadDAO)

        val søknaderHentetFraRepository = repository.findAllByAktørId(aktørIdSomIkkeEksisterer)

        assert(søknaderHentetFraRepository.isEmpty())
    }

    @Test
    fun `Sjekke som søknad eksisterer basert på aktørId og journalpostId`(){
        val søknadDAO = lagSøknadDAO()

        repository.save(søknadDAO)

        val eksistererSøknad = repository.existsSøknadDAOByAktørIdAndJournalpostId(aktørId, journalpostId)

        assert(eksistererSøknad)
    }

    @Test
    fun `Sjekke om søknad eksisterer ved bruk av aktørId som ikke eksisterer`(){
        val søknadDAO = lagSøknadDAO()
        repository.save(søknadDAO)

        val eksistererSøknad = repository.existsSøknadDAOByAktørIdAndJournalpostId(aktørIdSomIkkeEksisterer, journalpostId)

        assertFalse(eksistererSøknad)
    }

    @Test
    fun `Lagrer to ulike søknader med forskjellig journalpostId men lik aktørId`(){
        var søknadDAO = lagSøknadDAO()
        repository.save(søknadDAO)
        var eksistererSøknad = repository.existsSøknadDAOByAktørIdAndJournalpostId(aktørId, journalpostId)
        assertTrue(eksistererSøknad)

        val journalpostIdSomIkkeEksisterer = "222222"
        eksistererSøknad = repository.existsSøknadDAOByAktørIdAndJournalpostId(aktørId, journalpostIdSomIkkeEksisterer)
        assertFalse(eksistererSøknad)

        val ulikJournalpostId = "1111111"
        søknadDAO = lagSøknadDAO(customJournalpostId = ulikJournalpostId)
        repository.save(søknadDAO)
        eksistererSøknad = repository.existsSøknadDAOByAktørIdAndJournalpostId(aktørId, ulikJournalpostId)
        assertTrue(eksistererSøknad)
    }


    private fun lagSøknadDAO(
            customAktørId: AktørId = aktørId,
            customJournalpostId: String = journalpostId): SøknadDAO = SøknadDAO(
            id = UUID.randomUUID(),
            aktørId = customAktørId,
            fødselsnummer = fødselsnummer,
            søknadstype = Søknadstype.OMP_UTBETALING_SNF,
            status = SøknadsStatus.MOTTATT,
            journalpostId = customJournalpostId,
            saksId = "2222",
            opprettet = LocalDateTime.now(),
            søknad =
            //language=json
            """
                    {
                        "søknadId": "05ce3630-76eb-40f4-87a3-a5d55af58e40",
                        "språk": "nb"
                    }
                    """.trimIndent()
    )
}
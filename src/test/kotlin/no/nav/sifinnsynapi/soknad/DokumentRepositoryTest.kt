package no.nav.sifinnsynapi.soknad

import assertk.assertions.hasSameSizeAs
import assertk.assertions.isNotNull
import no.nav.security.token.support.test.spring.TokenGeneratorConfiguration
import no.nav.sifinnsynapi.dokument.DokumentDAO
import no.nav.sifinnsynapi.dokument.DokumentRepository
import no.nav.sifinnsynapi.utils.file2ByteArray
import org.junit.Assert.assertNotNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*

@DataJpaTest
@ActiveProfiles("test")
@Import(TokenGeneratorConfiguration::class) // Tilgjengliggjør en oicd-provider for test. Se application-test.yml -> no.nav.security.jwt.issuer.selvbetjening for konfigurasjon
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension::class)
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class DokumentRepositoryTest {

    @Autowired
    lateinit var repository: DokumentRepository // Repository som brukes til databasekall.

    @BeforeAll
    internal fun setUp() {
        assertNotNull(repository)
    }

    @AfterEach
    internal fun tearDown() {
        repository.deleteAll() //Tømmer databasen mellom hver test
    }

    @Test
    fun `Lagrer dokument i repository og henter opp basert på søknadId`() {
        val søknadId = UUID.randomUUID()
        val filSombyteArray = file2ByteArray("eksempel-søknad.pdf")

        val dokumentDAO = repository.save(DokumentDAO(
                id = UUID.randomUUID(),
                søknadId = søknadId,
                innhold = filSombyteArray
        ))

        val dokumentHentetFraRepository = repository.findBySøknadId(søknadId)
        assertNotNull(dokumentHentetFraRepository)
        assertk.assertThat(dokumentHentetFraRepository!!.innhold)
                .isNotNull()
                .hasSameSizeAs(filSombyteArray)
    }
}

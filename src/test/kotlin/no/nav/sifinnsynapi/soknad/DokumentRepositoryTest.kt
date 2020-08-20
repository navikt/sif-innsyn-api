package no.nav.sifinnsynapi.soknad

import assertk.assertions.hasSameSizeAs
import assertk.assertions.isNotNull
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
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.*

@DataJpaTest(properties = [
    "spring.cloud.gcp.core.enabled=false",
    "spring.cloud.gcp.secretmanager.enabled=false",
    "spring.datasource.url=jdbc:tc:postgresql:9.6:///",
    "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver"
])
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

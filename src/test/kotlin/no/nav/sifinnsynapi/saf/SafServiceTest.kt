package no.nav.sifinnsynapi.saf

import assertk.Result
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.runBlocking
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.sifinnsynapi.saf.generated.enums.Sakstype
import no.nav.sifinnsynapi.saf.generated.hentjournalpostinfo.Journalpost
import no.nav.sifinnsynapi.saf.generated.hentjournalpostinfo.Sak
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension

@Suppress("DEPRECATION")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["spring.main.allow-bean-definition-overriding=true"]
)
@AutoConfigureWireMock
@ActiveProfiles("test")
@ExtendWith(SpringExtension::class)
@EnableMockOAuth2Server // Tilgjengliggjør en oicd-provider for test. Se application-test.yml -> no.nav.security.jwt.issuer.selvbetjening for konfigurasjon
internal class SafServiceTest {

    @Autowired
    lateinit var safService: SafService

    @Test
    internal fun `happy case`() {
        runBlocking {
            val forventetJournalpostinfo = Journalpost(
                sak = Sak(
                    fagsakId = "1DM8RSQ",
                    sakstype = Sakstype.FAGSAK,
                    fagsaksystem = "K9",
                    datoOpprettet = "2021-04-16T11:02:30"
                )
            )

            assertThat {
                val hentJournalpostinfo: Journalpost = safService.hentJournalpostinfo("123456")
                hentJournalpostinfo
            }.isEqualTo(Result.success(forventetJournalpostinfo))
        }
    }
}

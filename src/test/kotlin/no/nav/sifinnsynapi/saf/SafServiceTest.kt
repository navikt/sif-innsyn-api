package no.nav.sifinnsynapi.saf

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.sifinnsynapi.saf.generated.enums.Sakstype
import no.nav.sifinnsynapi.saf.generated.hentjournalpostinfo.Journalpost
import no.nav.sifinnsynapi.saf.generated.hentjournalpostinfo.Sak
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock
import org.wiremock.spring.InjectWireMock

@Suppress("DEPRECATION")
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["spring.main.allow-bean-definition-overriding=true"]
)
@EnableWireMock(ConfigureWireMock())
@ActiveProfiles("test")
@ExtendWith(SpringExtension::class)
@EnableMockOAuth2Server // Tilgjengliggjør en oicd-provider for test. Se application-test.yml -> no.nav.security.jwt.issuer.selvbetjening for konfigurasjon
internal class SafServiceTest {

    @Autowired
    lateinit var safService: SafService

    @InjectWireMock
    lateinit var wireMockServer: WireMockServer

    @Test
    fun `happy case`() {
        val forventetJournalpostinfo = Journalpost(
            sak = Sak(
                fagsakId = "1DM8RSQ",
                sakstype = Sakstype.FAGSAK,
                fagsaksystem = "K9",
                datoOpprettet = "2021-04-16T11:02:30"
            )
        )

        assertThat(runCatching<Journalpost> {
            val hentJournalpostinfo: Journalpost = safService.hentJournalpostinfo("123456")
            hentJournalpostinfo
        }).isEqualTo(Result.success(forventetJournalpostinfo))
    }

    @Test
    fun `skal retry ved feil og til slutt lykkes`() {
        // Arrange - Setup scenario hvor første kall returnerer GraphQL error, andre kall lykkes
        wireMockServer.resetAll()

        // Første kall - returnerer GraphQL error (som trigger IllegalStateException i SafService)
        wireMockServer.stubFor(
            post(urlEqualTo("/saf-api-mock/graphql"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Started")
                .withRequestBody(matchingJsonPath("$.query"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "data": {
                                "journalpost": null
                              },
                              "errors": [
                                {
                                  "message": "Internal Server Error",
                                  "extensions": {
                                    "code": "INTERNAL_SERVER_ERROR"
                                  }
                                }
                              ]
                            }
                        """.trimIndent())
                )
                .willSetStateTo("First Attempt Failed")
        )

        // Andre kall - lykkes
        wireMockServer.stubFor(
            post(urlEqualTo("/saf-api-mock/graphql"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("First Attempt Failed")
                .withRequestBody(matchingJsonPath("$.query"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "data": {
                                "journalpost": {
                                  "sak": {
                                    "fagsakId": "1DM8RSQ",
                                    "sakstype": "FAGSAK",
                                    "fagsaksystem": "K9",
                                    "datoOpprettet": "2021-04-16T11:02:30"
                                  }
                                }
                              }
                            }
                        """.trimIndent())
                )
                .willSetStateTo("Success")
        )

        // Act & Assert
        val forventetJournalpostinfo = Journalpost(
            sak = Sak(
                fagsakId = "1DM8RSQ",
                sakstype = Sakstype.FAGSAK,
                fagsaksystem = "K9",
                datoOpprettet = "2021-04-16T11:02:30"
            )
        )

        val result = safService.hentJournalpostinfo("999999")

        assertThat(result).isEqualTo(forventetJournalpostinfo)

        // Verifiser at det ble gjort 2 kall (1 feil + 1 retry)
        wireMockServer.verify(2, postRequestedFor(urlEqualTo("/saf-api-mock/graphql")))
    }

    @Test
    fun `skal retry ved nettverksfeil og til slutt lykkes`() {
        // Arrange - Setup scenario hvor første kall feiler med connection reset, andre kall lykkes
        wireMockServer.resetAll()

        // Første kall - simulerer nettverksfeil med connection reset
        wireMockServer.stubFor(
            post(urlEqualTo("/saf-api-mock/graphql"))
                .inScenario("Network Failure Scenario")
                .whenScenarioStateIs("Started")
                .withRequestBody(matchingJsonPath("$.variables.journalpostId", equalTo("888888")))
                .willReturn(
                    aResponse()
                        .withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)
                )
                .willSetStateTo("Network Failed Once")
        )

        // Andre kall - lykkes
        wireMockServer.stubFor(
            post(urlEqualTo("/saf-api-mock/graphql"))
                .inScenario("Network Failure Scenario")
                .whenScenarioStateIs("Network Failed Once")
                .withRequestBody(matchingJsonPath("$.variables.journalpostId", equalTo("888888")))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {
                              "data": {
                                "journalpost": {
                                  "sak": {
                                    "fagsakId": "ABC123",
                                    "sakstype": "FAGSAK",
                                    "fagsaksystem": "K9",
                                    "datoOpprettet": "2021-05-20T14:30:00"
                                  }
                                }
                              }
                            }
                        """.trimIndent())
                )
                .willSetStateTo("Network Success")
        )

        // Act & Assert
        val forventetJournalpostinfo = Journalpost(
            sak = Sak(
                fagsakId = "ABC123",
                sakstype = Sakstype.FAGSAK,
                fagsaksystem = "K9",
                datoOpprettet = "2021-05-20T14:30:00"
            )
        )

        val result = safService.hentJournalpostinfo("888888")

        assertThat(result).isEqualTo(forventetJournalpostinfo)

        // Verifiser at det ble gjort 2 kall (1 feil + 1 retry)
        wireMockServer.verify(2, postRequestedFor(urlEqualTo("/saf-api-mock/graphql"))
            .withRequestBody(matchingJsonPath("$.variables.journalpostId", equalTo("888888"))))
    }
}

package no.nav.sifinnsynapi.utils

import com.github.tomakehurst.wiremock.client.WireMock
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import java.net.InetAddress

fun stubForAktørId(aktørId: String, status: Int) {
    WireMock.stubFor(
        WireMock.get(WireMock.urlPathMatching("/k9-selvbetjening-oppslag-mock/meg.*"))
            .withHeader("Authorization", WireMock.matching(".*"))
            .withQueryParam("a", WireMock.equalTo("aktør_id"))
            .willReturn(
                WireMock.aResponse()
                    .withStatus(status)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(
                        //language=json
                        """
                            {
                                "aktør_id": "$aktørId"
                            }
                        """.trimIndent()
                    )
            )
    )
}

fun stubForLeaderElection(status: Int = 200) = WireMock.stubFor(
    WireMock.get(WireMock.urlEqualTo("/leader/"))
        .willReturn(
            WireMock.aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody(
                    // language=JSON
                    """
                                {
                                  "name": "${InetAddress.getLocalHost().hostName}"
                                }
                            """.trimIndent()
                )
        )
)


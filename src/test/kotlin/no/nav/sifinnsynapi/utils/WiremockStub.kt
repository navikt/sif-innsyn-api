package no.nav.sifinnsynapi.utils

import com.github.tomakehurst.wiremock.client.WireMock
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType

fun stubForAktørId(aktørId: String, status: Int) {
    WireMock.stubFor(WireMock.get(WireMock.urlPathMatching("/k9-selvbetjening-oppslag-mock/meg.*"))
            .withHeader("x-nav-apiKey", WireMock.matching(".*"))
            .withHeader("Authorization", WireMock.matching(".*"))
            .withQueryParam("a", WireMock.equalTo("aktør_id"))
            .willReturn(WireMock.aResponse()
                    .withStatus(status)
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .withBody(
                            //language=json
                            """
                            {
                                "aktør_id": "$aktørId"
                            }
                        """.trimIndent()
                    ))
    )
}

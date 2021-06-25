package no.nav.sifinnsynapi.sts

import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.time.LocalDateTime

@Service
class STSClient(
        @Qualifier("stsClient") val stsClient: RestTemplate
) {
    private var cachedOidcToken: Token? = null

    companion object {
        private val log: Logger = LoggerFactory.getLogger(STSClient::class.java)

        val stsUrl = UriComponentsBuilder
                .fromUriString("/rest/v1/sts/token")
                .queryParam("grant_type", "client_credentials")
                .queryParam("scope", "openid")
                .build()
    }

    fun oicdToken(): String {
        if (cachedOidcToken.shouldBeRenewed()) {
            cachedOidcToken = stsClient.getForEntity(stsUrl.toUriString(), Token::class.java).body
        }
        return cachedOidcToken!!.token
    }

    private fun Token?.shouldBeRenewed(): Boolean = this?.hasExpired() ?: true

    private data class Token(
            @JsonProperty(value = "access_token", required = true)
            val token: String,
            @JsonProperty(value = "token_type", required = true)
            val type: String,
            @JsonProperty(value = "expires_in", required = true)
            val expiresIn: Int
    ) {
        private val expirationTime: LocalDateTime = LocalDateTime.now().plusSeconds(expiresIn - 20L)

        fun hasExpired(): Boolean = expirationTime.isBefore(LocalDateTime.now())
    }
}


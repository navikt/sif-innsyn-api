package no.nav.sifinnsynapi.saf

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.sifinnsynapi.util.HttpHeaderConstants
import no.nav.sifinnsynapi.util.MDCUtil
import no.nav.sifinnsynapi.util.WebClientUtils.requestLoggerFilter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class SafClientConfig(
    oauth2Config: ClientConfigurationProperties,
    private val oAuth2AccessTokenService: OAuth2AccessTokenService,
    @Value("\${no.nav.gateways.saf-base-url}") private val safBaseUrl: String,
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(SafClientConfig::class.java)
    }

    private val azureSafClientProperties = oauth2Config.registration["azure-saf"]
        ?: throw RuntimeException("could not find oauth2 client config for azure-saf")

    @Bean("safClient")
    fun client() = GraphQLWebClient(
        url = "$safBaseUrl/graphql",
        builder = WebClient.builder()
            .requestLoggerFilter(logger)
            .defaultRequest {
                it.header(AUTHORIZATION, "Bearer ${accessToken(azureSafClientProperties)}")
                val correlationId = MDCUtil.callIdOrNew()
                it.header(HttpHeaderConstants.NAV_CALL_ID, correlationId)
                it.header(HttpHeaderConstants.X_CORRELATION_ID, correlationId)
            }
    )

    private fun accessToken(clientProperties: ClientProperties): String {
        return oAuth2AccessTokenService.getAccessToken(clientProperties).access_token!!
    }
}

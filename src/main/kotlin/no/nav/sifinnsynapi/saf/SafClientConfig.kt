package no.nav.sifinnsynapi.saf

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.sifinnsynapi.util.HttpHeaderConstants
import no.nav.sifinnsynapi.util.MDCUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.http.client.HttpClientRequest
import reactor.netty.http.client.HttpClientResponse

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
            .clientConnector(
                ReactorClientHttpConnector(
                    HttpClient.create()
                        .doOnRequest { request: HttpClientRequest, _ ->
                            logger.info("{} {} {}", request.version(), request.method(), request.resourceUrl())
                        }
                        .doOnResponse { response: HttpClientResponse, _ ->
                            logger.info(
                                "{} - {} {} {}",
                                response.status().toString(),
                                response.version(),
                                response.method(),
                                response.resourceUrl()
                            )
                        }
                )
            )
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

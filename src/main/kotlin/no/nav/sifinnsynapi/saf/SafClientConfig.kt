package no.nav.sifinnsynapi.saf

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import io.netty.channel.ChannelOption
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
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.netty.http.HttpProtocol
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.time.Duration

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

    /**
     * Egen ConnectionProvider med idle-eviction for å unngå resirkulering av døde connections
     */
    @Bean
    fun safConnectionProvider(): ConnectionProvider =
        ConnectionProvider.builder("saf-connection-pool")
            .maxConnections(200)
            .maxIdleTime(Duration.ofSeconds(25))       // kortere enn LB keep-alive
            .evictInBackground(Duration.ofSeconds(60)) // rydder jevnlig
            .build()

    @Bean
    fun safHttpClient(safConnectionProvider: ConnectionProvider): HttpClient =
        HttpClient.create(safConnectionProvider)
            .protocol(HttpProtocol.HTTP11)
            .responseTimeout(Duration.ofSeconds(15))
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)

    @Bean("safClient")
    fun client(safHttpClient: HttpClient) = GraphQLWebClient(
        url = "$safBaseUrl/graphql",
        builder = WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(safHttpClient))
            .filters { filters ->
                filters.add { request, next ->
                    logger.info("---> {} {}", request.method(), request.url())
                    next.exchange(request).also { it: Mono<ClientResponse> ->
                        it.subscribe {
                            logger.info("<--- {} for {} {}", it.statusCode().value(), request.method(), request.url())
                        }
                    }
                }
            }
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

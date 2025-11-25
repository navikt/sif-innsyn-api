package no.nav.sifinnsynapi.safselvbetjening

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import io.netty.channel.ChannelOption
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.sifinnsynapi.util.HttpHeaderConstants.NAV_CALL_ID
import no.nav.sifinnsynapi.util.HttpHeaderConstants.X_CORRELATION_ID
import no.nav.sifinnsynapi.util.MDCUtil
import no.nav.sifinnsynapi.util.WebClientUtils.requestLoggerFilter
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager
import org.apache.hc.core5.util.TimeValue
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import java.time.Duration
import java.util.*
import java.util.function.Supplier

@Configuration
class SafSelvbetjeningClientsConfig(
    oauth2Config: ClientConfigurationProperties,
    private val oAuth2AccessTokenService: OAuth2AccessTokenService,
    @Value("\${no.nav.gateways.saf-selvbetjening-base-url}") private val safSelvbetjeningBaseUrl: String,
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(SafSelvbetjeningClientsConfig::class.java)
    }

    private val tokenxSafSelvbetjeningClientProperties = oauth2Config.registration["tokenx-safselvbetjening"]
        ?: throw RuntimeException("could not find oauth2 client config for tokenx-safselvbetjening")

    @Bean("safSelvbetjeningGraphQLClient")
    fun graphQLClient() = GraphQLWebClient(
        url = "${safSelvbetjeningBaseUrl}/graphql",
        builder = WebClient.builder()
            .clientConnector(
                ReactorClientHttpConnector(
                    HttpClient.create(
                        ConnectionProvider.builder("saf-selvbetjening-pool")
                            .maxConnections(100)
                            .maxIdleTime(Duration.ofMinutes(55))    // Stay below 60-min firewall timeout
                            .maxLifeTime(Duration.ofMinutes(55))    // Connection TTL for DNS refresh
                            .pendingAcquireMaxCount(50)  // Max requests waiting for connection
                            .evictInBackground(Duration.ofMinutes(5))  // Periodic cleanup of stale connections
                            .build()
                    )
                        .responseTimeout(Duration.ofSeconds(40))  // Max time to wait for complete response (cross-cluster)
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)  // 10s connection timeout (cross-cluster)
                        .option(ChannelOption.SO_KEEPALIVE, true)  // Enable TCP keep-alive to detect dead connections
                )
            )
            .requestLoggerFilter(logger)
            .defaultRequest {
                val correlationId = MDCUtil.callIdOrNew()
                it.header(NAV_CALL_ID, correlationId)
                it.header(X_CORRELATION_ID, correlationId)
                it.header(
                    AUTHORIZATION,
                    "Bearer ${oAuth2AccessTokenService.getAccessToken(tokenxSafSelvbetjeningClientProperties).access_token}"
                )
            }
    )


    @Bean
    fun safSelvbetjeningRestTemplate(
        restTemplateBuilder: RestTemplateBuilder,
        clientConfigurationProperties: ClientConfigurationProperties,
        oAuth2AccessTokenService: OAuth2AccessTokenService,
    ): RestTemplate {
        logger.info("Konfigurerer opp tokenx klient for safselvbetjening.")

        val connectionManager = PoolingHttpClientConnectionManager().apply {
            maxTotal = 100
            defaultMaxPerRoute = 20
            setValidateAfterInactivity(TimeValue.ofSeconds(5))
        }

        val httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .evictIdleConnections(TimeValue.ofMinutes(5))              // Short TTL for external services
            .setKeepAliveStrategy { response, context -> TimeValue.ofMinutes(55) }  // Stay below 60-min firewall timeout
            .evictExpiredConnections()
            .build()

        val requestFactory = HttpComponentsClientHttpRequestFactory(httpClient).apply {
            setConnectTimeout(Duration.ofSeconds(10))              // Connection timeout (external services recommendation)
            setConnectionRequestTimeout(Duration.ofSeconds(45))
            setReadTimeout(Duration.ofSeconds(40))
        }

        return restTemplateBuilder
            .requestFactory(Supplier { requestFactory })
            .rootUri(safSelvbetjeningBaseUrl)
            .defaultHeader(NAV_CALL_ID, UUID.randomUUID().toString())
            .additionalInterceptors(
                bearerTokenInterceptor(
                    tokenxSafSelvbetjeningClientProperties,
                    oAuth2AccessTokenService
                ), requestLoggerInterceptor()
            )
            .build()
    }

    private fun bearerTokenInterceptor(
        clientProperties: ClientProperties,
        oAuth2AccessTokenService: OAuth2AccessTokenService,
    ): ClientHttpRequestInterceptor {
        return ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution ->
            val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
            request.headers.setBearerAuth(response.access_token!!)
            execution.execute(request, body)
        }
    }

    private fun requestLoggerInterceptor() =
        ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution ->
            logger.info("{} {}", request.method, request.uri)
            execution.execute(request, body)
        }
}

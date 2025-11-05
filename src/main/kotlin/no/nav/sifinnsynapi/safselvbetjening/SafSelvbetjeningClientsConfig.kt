package no.nav.sifinnsynapi.safselvbetjening

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.sifinnsynapi.util.HttpHeaderConstants.NAV_CALL_ID
import no.nav.sifinnsynapi.util.HttpHeaderConstants.X_CORRELATION_ID
import no.nav.sifinnsynapi.util.MDCUtil
import no.nav.sifinnsynapi.util.WebClientUtils.requestLoggerFilter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.resources.ConnectionProvider
import java.time.Duration
import java.util.*

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

    /**
     * Egen ConnectionProvider med idle-eviction for å unngå resirkulering av døde connections
     */
    @Bean
    fun safSelvbetjeningConnectionProvider(): ConnectionProvider =
        ConnectionProvider.builder("saf-selvbetjening-connection-pool")
            .maxConnections(200)
            .maxIdleTime(Duration.ofSeconds(25))       // kortere enn LB keep-alive
            .evictInBackground(Duration.ofSeconds(60)) // rydder jevnlig
            .build()

    @Bean("safSelvbetjeningGraphQLClient")
    fun graphQLClient() = GraphQLWebClient(
        url = "${safSelvbetjeningBaseUrl}/graphql",
        builder = WebClient.builder()
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
        return restTemplateBuilder
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

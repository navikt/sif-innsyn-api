package no.nav.sifinnsynapi.k9sakinnsynapi

import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.sifinnsynapi.filter.MDCValuesPropagatingClienHttpRequesInterceptor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpRequest
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Configuration
class K9SakInnsynApiClientConfig(
    @Value("\${no.nav.gateways.k9-sak-innsyn-api-base-url}") private val k9SakInnsynApiBaseUrl: String,
    oauth2Config: ClientConfigurationProperties,
    private val oAuth2AccessTokenService: OAuth2AccessTokenService
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(K9SakInnsynApiClientConfig::class.java)
        const val TOKEN_X_K9_SAK_INNSYN_API = "tokenx-k9-sak-innsyn-api"
    }

    private val tokenxK9SakInnsynApiClientProperties =
        oauth2Config.registration[TOKEN_X_K9_SAK_INNSYN_API]
            ?: throw RuntimeException("could not find oauth2 client config for $TOKEN_X_K9_SAK_INNSYN_API")

    @Bean(name = ["k9SakInnsynApiClient"])
    fun restTemplate(
        builder: RestTemplateBuilder,
        mdcInterceptor: MDCValuesPropagatingClienHttpRequesInterceptor
    ): RestTemplate {
        return builder
            .connectTimeout(Duration.ofSeconds(20))
            .readTimeout(Duration.ofSeconds(20))
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .rootUri(k9SakInnsynApiBaseUrl)
            .defaultMessageConverters()
            .interceptors(bearerTokenInterceptor(), mdcInterceptor, requestLoggerInterceptor())
            .build()
    }

    private fun bearerTokenInterceptor(): ClientHttpRequestInterceptor {
        return ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution ->
            val response = oAuth2AccessTokenService.getAccessToken(tokenxK9SakInnsynApiClientProperties)
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

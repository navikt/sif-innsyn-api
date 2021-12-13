package no.nav.sifinnsynapi.k9sakinnsynapi

import no.nav.security.token.support.spring.validation.interceptor.BearerTokenClientHttpRequestInterceptor
import no.nav.sifinnsynapi.http.MDCValuesPropagatingClienHttpRequesInterceptor
import no.nav.sifinnsynapi.util.Constants
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpRequest
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.web.client.RestTemplate
import java.time.Duration
import java.util.*

@Configuration
class K9SakInnsynApiClientConfig(
    @Value("\${no.nav.gateways.k9-sak-innsyn-api-base-url}") private val k9SakInnsynApiBaseUrl: String
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(K9SakInnsynApiClientConfig::class.java)
    }

    @Bean(name = ["k9SakInnsynApiClient"])
    fun restTemplate(
        builder: RestTemplateBuilder,
        bearerTokenClientHttpRequestInterceptor: BearerTokenClientHttpRequestInterceptor,
        mdcInterceptor: MDCValuesPropagatingClienHttpRequesInterceptor
    ): RestTemplate {
        return builder
            .setConnectTimeout(Duration.ofSeconds(20))
            .setReadTimeout(Duration.ofSeconds(20))
            .defaultHeader(Constants.X_CORRELATION_ID, UUID.randomUUID().toString())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .rootUri(k9SakInnsynApiBaseUrl)
            .defaultMessageConverters()
            .interceptors(bearerTokenClientHttpRequestInterceptor, mdcInterceptor, requestLoggerInterceptor())
            .build()
    }

    private fun requestLoggerInterceptor() =
        ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution ->
            logger.info("{} {}", request.method, request.uri)
            execution.execute(request, body)
        }

}

package no.nav.sifinnsynapi.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import no.nav.security.token.support.spring.validation.interceptor.BearerTokenClientHttpRequestInterceptor
import no.nav.sifinnsynapi.http.MDCValuesPropagatingClienHttpRequesInterceptor
import no.nav.sifinnsynapi.util.Constants.X_CORRELATION_ID
import no.nav.sifinnsynapi.util.Constants.X_NAV_APIKEY
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.retry.RetryCallback
import org.springframework.retry.RetryContext
import org.springframework.retry.RetryListener
import org.springframework.web.client.RestTemplate
import java.util.*

@Configuration
class RestTemplateConfig(
        @Value("\${no.nav.gateways.k9-selvbetjening-oppslag}")
        private val oppslagsUrl: String,
        private val apigwConfig: ApiGwApiKeyConfig,
        @Value("\${spring.rest.retry.maxAttempts}")
        private val maxAttempts: Int
): RetryListener {

    private companion object{
        val logger: Logger = LoggerFactory.getLogger(RestTemplateConfig::class.java)
    }
    @Bean(name = ["k9OppslagsKlient"])
    fun restTemplate(builder: RestTemplateBuilder, tokenInterceptor: BearerTokenClientHttpRequestInterceptor, mdcInterceptor: MDCValuesPropagatingClienHttpRequesInterceptor): RestTemplate {
        return builder
                .additionalMessageConverters(MappingJackson2HttpMessageConverter(k9SelvbetjeningOppslagKonfigurert()))
                .defaultHeader(X_NAV_APIKEY, apigwConfig.apiKey)
                .defaultHeader(X_CORRELATION_ID, UUID.randomUUID().toString())
                .rootUri(oppslagsUrl)
                .interceptors(tokenInterceptor, mdcInterceptor)
                .build()
    }

    override fun <T : Any, E : Throwable> open(context: RetryContext, callback: RetryCallback<T, E>): Boolean {
        logger.warn("Feiler ved utgående rest-kall, kjører retry")
        return true
    }

    override fun <T : Any, E : Throwable?> close(context: RetryContext, callback: RetryCallback<T, E>, throwable: Throwable?) {
        logger.info("Gir opp etter {} av {} forsøk",
                context.retryCount, maxAttempts)
    }

    override fun <T : Any, E : Throwable> onError(context: RetryContext, callback: RetryCallback<T, E>, throwable: Throwable) {
        val currentTry = context.retryCount
        val contextString = context.getAttribute("context.name") as String

        logger.warn("Forsøker {} av {}, {}", currentTry, maxAttempts, contextString.split(" ")[2])
    }
}

internal fun k9SelvbetjeningOppslagKonfigurert(): ObjectMapper {
    return ObjectMapper().apply {
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)
        registerModule(JavaTimeModule())
    }
}



package no.nav.sifinnsynapi.config

import no.nav.security.token.support.spring.validation.interceptor.BearerTokenClientHttpRequestInterceptor
import no.nav.sifinnsynapi.http.MDCValuesPropagatingClienHttpRequesInterceptor
import no.nav.sifinnsynapi.util.Constants.X_CORRELATION_ID
import no.nav.sifinnsynapi.util.Constants.X_NAV_APIKEY
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import java.util.*

@Configuration
class RestTemplateConfig(
        @Value("\${no.nav.gateways.k9-selvbetjening-oppslag}")
        private val oppslagsUrl: String,
        private val apigwConfig: ApiGwApiKeyConfig
) {

    @Bean(name = ["k9OppslagsKlient"])
    fun restTemplate(builder: RestTemplateBuilder, tokenInterceptor: BearerTokenClientHttpRequestInterceptor, mdcInterceptor: MDCValuesPropagatingClienHttpRequesInterceptor): RestTemplate {
        return builder
                .defaultHeader(X_NAV_APIKEY, apigwConfig.apiKey)
                .defaultHeader(X_CORRELATION_ID, UUID.randomUUID().toString())
                .rootUri(oppslagsUrl)
                .interceptors(tokenInterceptor, mdcInterceptor)
                .build()
    }
}

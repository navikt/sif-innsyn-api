package no.nav.sifinnsynapi.oppslag

import assertk.assertThat
import assertk.assertions.isNotNull
import no.nav.security.token.support.spring.validation.interceptor.BearerTokenClientHttpRequestInterceptor
import no.nav.security.token.support.test.spring.TokenGeneratorConfiguration
import no.nav.sifinnsynapi.config.ApiGwApiKeyConfig
import no.nav.sifinnsynapi.http.MDCValuesPropagatingClienHttpRequesInterceptor
import no.nav.sifinnsynapi.util.Constants
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.RestTemplate
import java.time.Duration
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = ["spring.main.allow-bean-definition-overriding=true"])
@AutoConfigureWireMock(port = 8000)
@ActiveProfiles("test")
@Import(TokenGeneratorConfiguration::class)
internal class OppslagsServiceTest {

    @TestConfiguration
    class TestConfig {
        @Autowired
        lateinit var apiGwApiKeyConfig: ApiGwApiKeyConfig

        @Value("\${no.nav.gateways.k9-selvbetjening-oppslag}")
        lateinit var oppslagsUrl: String

        @Bean(name = ["k9OppslagsKlient"])
        @Primary
        fun restTemplate(builder: RestTemplateBuilder, tokenInterceptor: BearerTokenClientHttpRequestInterceptor, mdcInterceptor: MDCValuesPropagatingClienHttpRequesInterceptor): RestTemplate {
            return builder
                    .setConnectTimeout(Duration.ofSeconds(20))
                    .setReadTimeout(Duration.ofSeconds(20))
                    .defaultHeader(Constants.X_NAV_APIKEY, apiGwApiKeyConfig.apiKey)
                    .defaultHeader(Constants.X_CORRELATION_ID, UUID.randomUUID().toString())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, anyString())
                    .defaultMessageConverters()
                    .rootUri(oppslagsUrl)
                    .build()
        }
    }

    @Autowired
    lateinit var oppslagsService: OppslagsService

    @Test
    fun hentAktørId() {
        val hentAktørId = oppslagsService.hentAktørId()
        assertThat(hentAktørId).isNotNull()
    }
}

package no.nav.sifinnsynapi.oppslag

import assertk.assertThat
import assertk.assertions.isNotNull
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.security.token.support.spring.validation.interceptor.BearerTokenClientHttpRequestInterceptor
import no.nav.sifinnsynapi.filter.MDCValuesPropagatingClienHttpRequesInterceptor
import no.nav.sifinnsynapi.util.HttpHeaderConstants
import no.nav.sifinnsynapi.utils.stubForAktørId
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.anyString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.web.client.RestTemplate
import java.time.Duration
import java.util.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = ["spring.main.allow-bean-definition-overriding=true"])
@AutoConfigureWireMock
@ActiveProfiles("test")
@EnableMockOAuth2Server // Tilgjengliggjør en oicd-provider for test. Se application-test.yml -> no.nav.security.jwt.issuer.selvbetjening for konfigurasjon
internal class OppslagsServiceTest {

    @TestConfiguration
    class TestConfig {

        @Value("\${no.nav.gateways.k9-selvbetjening-oppslag}")
        lateinit var oppslagsUrl: String

        @Bean(name = ["k9OppslagsKlient"])
        @Primary
        fun restTemplate(builder: RestTemplateBuilder, tokenInterceptor: BearerTokenClientHttpRequestInterceptor, mdcInterceptor: MDCValuesPropagatingClienHttpRequesInterceptor): RestTemplate {
            return builder
                    .connectTimeout(Duration.ofSeconds(20))
                    .readTimeout(Duration.ofSeconds(20))
                    .defaultHeader(HttpHeaderConstants.X_CORRELATION_ID, UUID.randomUUID().toString())
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

    @Test
    fun `gitt http 451 feil, forvent TilgangNektetException`() {
        stubForAktørId("123", HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS.value())
        assertThrows<TilgangNektetException> { oppslagsService.hentAktørId() }
    }
}

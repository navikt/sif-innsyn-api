package no.nav.sifinnsynapi.config

import no.nav.security.token.support.client.core.ClientProperties
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import no.nav.sifinnsynapi.util.Constants.NAV_CALL_ID
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.web.client.RestTemplate
import java.util.*


@EnableOAuth2Client(cacheEnabled = true)
@Configuration
class TokenClientConfig(
    @Value("\${no.nav.gateways.saf-selvbetjening-base-url}") private val safSelvbetjeningBaseUrl: String
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(TokenClientConfig::class.java)
    }

    @Bean
    fun tokenxSafSelvbetjeningClient(
        restTemplateBuilder: RestTemplateBuilder,
        clientConfigurationProperties: ClientConfigurationProperties,
        oAuth2AccessTokenService: OAuth2AccessTokenService
    ): RestTemplate {
        val clientProperties: ClientProperties =
            clientConfigurationProperties.registration["tokenx-safselvbetjening"]
                ?: throw RuntimeException("could not find oauth2 client config for tokenx-safselvbetjening")

        logger.info("Konfigurerer opp tokenx klient for safselvbetjening.")
        return restTemplateBuilder
            .rootUri(safSelvbetjeningBaseUrl)
            .defaultHeader(NAV_CALL_ID, UUID.randomUUID().toString())
            .additionalInterceptors(bearerTokenInterceptor(clientProperties, oAuth2AccessTokenService))
            .build()
    }

    private fun bearerTokenInterceptor(
        clientProperties: ClientProperties,
        oAuth2AccessTokenService: OAuth2AccessTokenService
    ): ClientHttpRequestInterceptor {
        return ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution ->
            val response = oAuth2AccessTokenService.getAccessToken(clientProperties)
            request.headers.setBearerAuth(response.accessToken)
            execution.execute(request, body)
        }
    }
}

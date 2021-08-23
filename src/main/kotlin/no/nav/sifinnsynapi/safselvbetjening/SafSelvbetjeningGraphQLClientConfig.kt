package no.nav.sifinnsynapi.safselvbetjening

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class SafSelvbetjeningGraphQLClientConfig(
    private val oAuth2AccessTokenService: OAuth2AccessTokenService,
    oauth2Config: ClientConfigurationProperties,
    @Value("\${no.nav.gateways.saf-selvbetjening-base-url}") private val safSelvbetjeningBaseUrl: String
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(SafSelvbetjeningGraphQLClientConfig::class.java)
    }

    private val tokenxSafSelvbetjeningClientProperties = oauth2Config.registration["tokenx-safselvbetjening"]
        ?: throw RuntimeException("could not find oauth2 client config for tokenx-safselvbetjening")

    @Bean("safSelvbetjeningGraphQLClient")
    fun client() = GraphQLWebClient(
        url = "${safSelvbetjeningBaseUrl}/graphql",
        builder = WebClient.builder()
            .defaultRequest {
                val accessToken =
                    oAuth2AccessTokenService.getAccessToken(tokenxSafSelvbetjeningClientProperties).accessToken

                logger.info("Exchanger sluttbrukertoken mot tokenx accesstoken: {}", accessToken)

                it.header(
                    AUTHORIZATION,
                    accessToken
                )
            }
    )
}

package no.nav.sifinnsynapi.safselvbetjening

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.sifinnsynapi.util.Constants.NAV_CALL_ID
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.web.reactive.function.client.WebClient
import java.util.*

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
                    oAuth2AccessTokenService.getAccessToken(tokenxSafSelvbetjeningClientProperties)

                val jwtToken = JwtToken(accessToken.accessToken)
                jwtToken.jwtTokenClaims.allClaims["pid"] = jwtToken.subject

                logger.info("Exchanger sluttbrukertoken mot tokenx accesstoken: {}", accessToken)

                it.header(
                    AUTHORIZATION,
                    jwtToken.tokenAsString
                )
                it.header(NAV_CALL_ID, UUID.randomUUID().toString())
            }
    )
}

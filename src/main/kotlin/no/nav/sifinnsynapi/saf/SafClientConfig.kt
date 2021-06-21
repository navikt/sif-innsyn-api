package no.nav.sifinnsynapi.saf

import no.nav.sifinnsynapi.sts.STSClient
import org.springframework.beans.factory.annotation.Value
import com.expediagroup.graphql.client.spring.GraphQLWebClient
import no.nav.sifinnsynapi.util.asAuthoriationHeader
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class SafClientConfig(
    private val stsClient: STSClient,
    @Value("\${no.nav.gateways.saf-base-url}") private val safBaseUrl: String
) {

    @Bean("safClient")
    fun client() = GraphQLWebClient(
        url = safBaseUrl,
        builder = WebClient.builder()
            .defaultRequest {
                it.header(AUTHORIZATION, stsClient.oicdToken().asAuthoriationHeader())
            }
    )
}

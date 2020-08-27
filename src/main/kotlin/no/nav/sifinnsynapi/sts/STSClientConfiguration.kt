package no.nav.sifinnsynapi.sts

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestTemplate
import java.time.Duration
import java.util.*

@Configuration
class STSClientConfiguration(
        @Value("\${no.nav.gateways.sts.url}") val stsUrl: String,
        @Value("\${no.nav.gateways.sts.username}") val username: String,
        @Value("\${no.nav.gateways.sts.password}") val password: String,
        @Value("\${no.nav.apigw.sts-apikey}") val apikey: String
) {

    @Bean(name = ["stsClient"])
    fun restTemplate(builder: RestTemplateBuilder): RestTemplate {
        return builder
                .rootUri(stsUrl)
                .setConnectTimeout(Duration.ofSeconds(20))
                .setReadTimeout(Duration.ofSeconds(20))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic ${credentials()}")
                .defaultHeader("x-nav-apiKey", apikey)
                .build()
    }

    private fun credentials() = Base64
            .getEncoder()
            .encodeToString("${username}:${password}".toByteArray(Charsets.UTF_8))
}

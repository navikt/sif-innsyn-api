package no.nav.sifinnsynapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
data class ApiGwApiKeyConfig(
        @Value("\${no.nav.apigw.apikey}")
        val apiKey: String
)

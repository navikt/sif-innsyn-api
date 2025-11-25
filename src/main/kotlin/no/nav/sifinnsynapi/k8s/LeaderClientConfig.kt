package no.nav.sifinnsynapi.k8s

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class LeaderClientConfig(@Value("\${ELECTOR_PATH}") private val electorPath: String) {
    private companion object {
        private val logger = LoggerFactory.getLogger(LeaderClientConfig::class.java)
    }

    @Bean
    fun leaderRestTemplate(restTemplateBuilder: RestTemplateBuilder): RestTemplate {
        logger.info("Konfigurerer opp klient for leader election.")
        return restTemplateBuilder
            .rootUri("http://$electorPath")
            .build()
    }
}

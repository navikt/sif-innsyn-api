package no.nav.sifinnsynapi.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@EnableWebMvc
class WebMvcConfig(
        @Value("\${no.nav.security.cors.allowed-origins}") val allowedOrigins: String
): WebMvcConfigurer {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(WebMvcConfigurer::class.java)
    }

    init {
        logger.info("Konfigurerer CORS...")
        logger.info("--- ALLOWED_ORIGINS={}", allowedOrigins)
    }

    /**
     * Configure cross origin requests processing.
     * @since 4.2
     */
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
                .allowedOrigins(*allowedOrigins.split(",").toTypedArray())
                .allowCredentials(true)

        super.addCorsMappings(registry)
    }
}

package no.nav.sifinnsynapi.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.zalando.problem.ProblemModule
import org.zalando.problem.violations.ConstraintViolationProblemModule

@Configuration
class WebMvcConfig(
    @Value("\${no.nav.security.cors.allowed-origins}") val allowedOrigins: String
) : WebMvcConfigurer {

    companion object {
        val log: Logger = LoggerFactory.getLogger(WebMvcConfigurer::class.java)
    }

    init {
        log.info("Konfigurerer CORS...")
        log.info("--- ALLOWED_ORIGINS={}", allowedOrigins)
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

    /**
     * Add handlers to serve static resources such as images, js, and, css
     * files from specific locations under web application root, the classpath,
     * and others.
     */
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {

        registry.addResourceHandler("swagger-ui.html")
            .addResourceLocations("classpath:/META-INF/resources/");

        registry.addResourceHandler("/webjars/**")
            .addResourceLocations("classpath:/META-INF/resources/webjars/");

        super.addResourceHandlers(registry)
    }

    @Bean
    fun problemModules(): ProblemModule {
        return ProblemModule()
    }

    @Bean
    fun violationProblemModule(): ConstraintViolationProblemModule {
        return ConstraintViolationProblemModule()
    }

    @Bean
    fun jacksonBuilderCustomizer(): Jackson2ObjectMapperBuilderCustomizer {
        log.info("-------> Customizing builder")
        return Jackson2ObjectMapperBuilderCustomizer { builder ->
            builder.featuresToDisable(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS
            )
            builder.propertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
        }
    }
}

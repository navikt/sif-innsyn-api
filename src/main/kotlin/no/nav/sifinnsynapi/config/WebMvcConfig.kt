package no.nav.sifinnsynapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.zalando.problem.ProblemModule
import org.zalando.problem.violations.ConstraintViolationProblemModule

@Configuration
@EnableWebMvc
class WebMvcConfig(
        @Value("\${no.nav.security.cors.allowed-origins}") val allowedOrigins: String,
        val mapper: ObjectMapper
) : WebMvcConfigurer {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(WebMvcConfigurer::class.java)
    }

    init {
        logger.info("Konfigurerer CORS...")
        logger.info("--- ALLOWED_ORIGINS={}", allowedOrigins)
    }

    /**
     * Configure the [HttpMessageConverters][HttpMessageConverter] to use for reading or writing
     * to the body of the request or response. If no converters are added, a
     * default list of converters is registered.
     *
     * **Note** that adding converters to the list, turns off
     * default converter registration. To simply add a converter without impacting
     * default registration, consider using the method
     * [.extendMessageConverters] instead.
     * @param converters initially an empty list of converters
     */
    override fun configureMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
        val builder = Jackson2ObjectMapperBuilder
                .json()
                .modules(ProblemModule(), ConstraintViolationProblemModule(), JavaTimeModule())

        builder.configure(mapper)
        val objectMapper = builder.build<ObjectMapper>()

        converters.add(MappingJackson2HttpMessageConverter(objectMapper))
        super.configureMessageConverters(converters)
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

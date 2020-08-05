package no.nav.sifinnsynapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverter
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

    override fun extendMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
        val jsonConverter = converters.filterIsInstance<MappingJackson2HttpMessageConverter>().first()
        converters.remove(jsonConverter)
        converters.add(mappingJackson2HttpMessageConverter())
        super.extendMessageConverters(converters)
    }

    fun mappingJackson2HttpMessageConverter(): MappingJackson2HttpMessageConverter {
        mapper.registerModules(ProblemModule(), ConstraintViolationProblemModule(), JavaTimeModule())
        logger.info("-------> {}", mapper.registeredModuleIds)
        return MappingJackson2HttpMessageConverter(mapper)
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

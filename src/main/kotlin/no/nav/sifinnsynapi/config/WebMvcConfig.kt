package no.nav.sifinnsynapi.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.security.token.support.core.jwt.JwtToken
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

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
            .allowedOrigins(*allowedOrigins.split(",").map { it.trim() }.toTypedArray())
            .allowCredentials(true)

        super.addCorsMappings(registry)
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

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(object : HandlerInterceptor {
            override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
                val authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION)?.replace("Bearer ", "")
                if (!authorizationHeader.isNullOrBlank()) {
                    val issuer = JwtToken(authorizationHeader).issuer
                    log.info("Issuer [$issuer]")
                }
                val method = request.method
                val requestURI = request.requestURI
                log.info("Request $method $requestURI")
                return super.preHandle(request, response, handler)
            }

            override fun postHandle(
                request: HttpServletRequest,
                response: HttpServletResponse,
                handler: Any,
                modelAndView: ModelAndView?
            ) {
                val status = response.status
                val method = request.method
                val requestURI = request.requestURI
                log.info("Response $status $method $requestURI")
                super.postHandle(request, response, handler, modelAndView)
            }
        })
        super.addInterceptors(registry)
    }
}

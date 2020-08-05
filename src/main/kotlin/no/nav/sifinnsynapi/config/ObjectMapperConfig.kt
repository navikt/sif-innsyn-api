package no.nav.sifinnsynapi.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.zalando.problem.ProblemModule
import org.zalando.problem.violations.ConstraintViolationProblemModule

@Configuration
/*
TODO: Spring ignorer på neo vis alle endringer man ønsker å gjøør for objectmapper.
Må feilsøkes. Pr. nå er modules konfigurert  her: no/nav/sifinnsynapi/config/WebMvcConfig.kt:41
 */
class ObjectMapperConfig {

   /* @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @Primary
    fun jacksonBuilderCustomizer(): Jackson2ObjectMapperBuilderCustomizer {
        return Jackson2ObjectMapperBuilderCustomizer { builder ->
            builder.featuresToEnable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            builder.featuresToEnable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            builder.featuresToEnable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)
            builder.propertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)
            builder.modules(ProblemModule(), ConstraintViolationProblemModule(), JavaTimeModule())
        }
    }*/

    /*@Bean
    @Primary
    fun jacksonObjectMapperBuilder(): Jackson2ObjectMapperBuilder {
        return Jackson2ObjectMapperBuilder
                .json()
                .modules(ProblemModule(), ConstraintViolationProblemModule(), JavaTimeModule())
                .featuresToEnable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .featuresToEnable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .featuresToEnable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)
                .propertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)
                .modules(ProblemModule(), ConstraintViolationProblemModule(), JavaTimeModule())
    }*/
}

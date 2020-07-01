package no.nav.sifinnsynapi.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.test.context.ActiveProfiles

@TestConfiguration
class ObjectMapperConfig {

    @Bean( name = ["testMapper"])
    fun configureMapper(): ObjectMapper = ObjectMapper().apply {
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)
        propertyNamingStrategy = PropertyNamingStrategy.LOWER_CAMEL_CASE
        registerModule(JavaTimeModule())
    }
}
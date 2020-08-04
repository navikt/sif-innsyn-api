package no.nav.sifinnsynapi

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.retry.annotation.EnableRetry
import org.springframework.transaction.annotation.EnableTransactionManagement

@SpringBootApplication(exclude = [
    ErrorMvcAutoConfiguration::class
])
@EnableJwtTokenValidation(ignore = ["org.springframework", "springfox.documentation"])
@EnableRetry
@EnableKafka
@EnableTransactionManagement
class SifInnsynApiApplication

fun main(args: Array<String>) {
    runApplication<SifInnsynApiApplication>(*args)
}

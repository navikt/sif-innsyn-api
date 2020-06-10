package no.nav.sifinnsynapi

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableJwtTokenValidation(ignore = ["org.springframework", "springfox.documentation"])
class SifInnsynApiApplication

fun main(args: Array<String>) {
	runApplication<SifInnsynApiApplication>(*args)
}

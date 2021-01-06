package no.nav.sifinnsynapi.config

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.context.annotation.Configuration

@EnableJwtTokenValidation(ignore = ["org.springframework", "springfox.documentation"])
@Configuration
internal class SecurityConfiguration

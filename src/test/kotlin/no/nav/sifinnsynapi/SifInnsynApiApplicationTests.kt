package no.nav.sifinnsynapi

import no.nav.security.token.support.test.spring.TokenGeneratorConfiguration
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
@Import(TokenGeneratorConfiguration::class)
class SifInnsynApiApplicationTests {

	@Test
	fun contextLoads() {
	}

}

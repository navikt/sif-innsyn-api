package no.nav.sifinnsynapi

import no.nav.security.token.support.test.spring.TokenGeneratorConfiguration
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
@Import(TokenGeneratorConfiguration::class)
@AutoConfigureWireMock
class SifInnsynApiApplicationTests {

	@Test
	fun contextLoads() {
	}

}

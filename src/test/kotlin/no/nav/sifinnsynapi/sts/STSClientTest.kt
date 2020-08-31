package no.nav.sifinnsynapi.sts

import assertk.assertThat
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNotNull
import no.nav.security.token.support.test.spring.TokenGeneratorConfiguration
import no.nav.sifinnsynapi.utils.stubStsToken
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension::class)
@ActiveProfiles("test")
@AutoConfigureWireMock(port = 8000)
@Import(TokenGeneratorConfiguration::class)
internal class STSClientTest {

    @Autowired
    lateinit var stsClient: STSClient

    @Test
    fun `Forvent å hente oicd-token`() {
        stubStsToken(forventetStatus = HttpStatus.OK, utgårOm = 3600)

        val forventetToken = stsClient.oicdToken()
        forventetToken.ikkeErNull()
    }

    @Test
    fun `Gitt at token er hentet allerede, forvent at den er cachet`() {
        stubStsToken(forventetStatus = HttpStatus.OK, utgårOm = 3600)

        val forventetToken = stsClient.oicdToken()
        val cachedToken = stsClient.oicdToken()
        cachedToken
                .ikkeErNull()
                .erLik(forventetToken)
    }

    private fun String.erLik(token: String): String {
        Assertions.assertEquals(token, this)
        return this
    }

    private fun String.ikkeErLik(token: String): String {
        assertThat(this).isNotEqualTo(token)
        return this
    }

    private fun String.ikkeErNull(): String {
        assertThat { this }.isNotNull()
        return this
    }
}

package no.nav.sifinnsynapi.forvaltning

import com.fasterxml.jackson.databind.ObjectMapper
import com.nimbusds.jose.JOSEObjectType.JWT
import com.nimbusds.jwt.JWTClaimsSet
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.sifinnsynapi.config.SecurityConfiguration
import no.nav.sifinnsynapi.sikkerhet.AuthorizationConfig
import no.nav.sifinnsynapi.sikkerhet.ContextHolder
import no.nav.sifinnsynapi.soknad.SøknadService
import no.nav.sifinnsynapi.util.CallIdGenerator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.net.URI
import java.util.*
import java.util.concurrent.TimeUnit.MINUTES

@ExtendWith(SpringExtension::class)
@EnableMockOAuth2Server
@Import(CallIdGenerator::class, SecurityConfiguration::class, AuthorizationConfig::class)
@WebMvcTest(controllers = [AktørBytteController::class])
@AutoConfigureMockMvc
@ActiveProfiles("test")
open class AktørBytteControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var mockOAuth2Server: MockOAuth2Server

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean(relaxed = true)
    private lateinit var søknadService: SøknadService


    private val PATH = "/forvaltning/oppdaterAktoerId"
    private val gyldigTestDriftRolle = "testdrift"
    private val ugyldigTestDriftRolle = "enugyldigrolle"


    @Test
    fun `skal autentisere med claims og tillate å endre aktørid`() {
        val gammelAktørId = "123"
        val nyAktørId = "567"
        every {
            søknadService.oppdaterAktørId(any(), any())
        } returns 2

        val token = token(
            ContextHolder.AZURE_AD,
            defaultJwtClaimsSetBuilder()
                .claim("NAVident", "TEST")
                .claim("groups", arrayOf(gyldigTestDriftRolle)) //definert i application.properties
                .build()
        )

        val requestBody = objectMapper.writeValueAsString(AktørBytteRequest(gammelAktørId, nyAktørId))

        mockMvc.perform(
            post(URI(PATH))
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.antallOppdaterteRader").value(2))

    }

    @Test
    fun `skal gi 403 dersom det mangler driftsrolle`() {
        val gammelAktørId = "123"
        val nyAktørId = "567"
        every {
            søknadService.oppdaterAktørId(any(), any())
        } returns 2

        val token = token(
            ContextHolder.AZURE_AD,
            defaultJwtClaimsSetBuilder()
                .claim("NAVident", "TEST")
                .claim("groups", arrayOf(ugyldigTestDriftRolle)) //definert i application.properties
                .build()
        )

        val requestBody = objectMapper.writeValueAsString(AktørBytteRequest(gammelAktørId, nyAktørId))

        mockMvc.perform(
            post(URI(PATH))
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        ).andExpect(status().isForbidden)
            .andExpect(
                content().json(
                    """
                        {
                        "type":"about:blank",
                        "title":"Forbidden",
                        "status":403,
                        "detail":"Mangler driftsrolle",
                        "instance":"/forvaltning/oppdaterAktoerId"
                        }
                    """.trimIndent(), true
                )
            )

    }

    private fun token(issuerId: String, jwtClaimsSet: JWTClaimsSet) = mockOAuth2Server.issueToken(
        issuerId,
        "theclientid",
        DefaultOAuth2TokenCallback(
            issuerId,
            "saksbehandler",
            JWT.type,
            listOf("aud-localhost"),
            jwtClaimsSet.claims,
            30L
        )
    ).serialize()


    private fun defaultJwtClaimsSetBuilder(): JWTClaimsSet.Builder {
        val now = Date()
        return JWTClaimsSet.Builder()
            .subject("testsub")
            .audience("aud-localhost")
            .jwtID(UUID.randomUUID().toString())
            .claim("auth_time", now)
            .notBeforeTime(now)
            .issueTime(now)
            .expirationTime(Date(now.time + MINUTES.toMillis(5)))
    }
}

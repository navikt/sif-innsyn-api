package no.nav.sifinnsynapi.forvaltning

import com.fasterxml.jackson.databind.ObjectMapper
import com.nimbusds.jose.JOSEObjectType.JWT
import com.nimbusds.jwt.JWTClaimsSet
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.sifinnsynapi.config.SecurityConfiguration
import no.nav.sifinnsynapi.dittnav.MicrofrontendAction
import no.nav.sifinnsynapi.dittnav.MicrofrontendId
import no.nav.sifinnsynapi.mikrofrontend.MikrofrontendDAO
import no.nav.sifinnsynapi.mikrofrontend.MikrofrontendService
import no.nav.sifinnsynapi.sikkerhet.AuthorizationConfig
import no.nav.sifinnsynapi.sikkerhet.ContextHolder
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.net.URI
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit.MINUTES

@ExtendWith(SpringExtension::class)
@EnableMockOAuth2Server
@Import(CallIdGenerator::class, SecurityConfiguration::class, AuthorizationConfig::class)
@WebMvcTest(controllers = [MikrofrontendForvaltningController::class])
@AutoConfigureMockMvc
@ActiveProfiles("test")
open class MikrofrontendForvaltningControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var mockOAuth2Server: MockOAuth2Server

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean(relaxed = true)
    private lateinit var mikrofrontendService: MikrofrontendService

    private val gyldigTestDriftRolle = "testdrift"
    private val ugyldigTestDriftRolle = "enugyldigrolle"

    @Test
    fun `skal hente mikrofrontender for gitt fødselsnummer`() {
        val fødselsnummer = "12345678901"
        val dao = MikrofrontendDAO(
            fødselsnummer = fødselsnummer,
            mikrofrontendId = MicrofrontendId.PLEIEPENGER_INNSYN.id,
            status = MicrofrontendAction.ENABLE,
            opprettet = ZonedDateTime.now(),
            behandlingsdato = null,
        )

        every { mikrofrontendService.findByFødselsnummer(fødselsnummer) } returns listOf(dao)

        val token = tokenMedDriftsrolle(gyldigTestDriftRolle)

        mockMvc.perform(
            get(URI("/forvaltning/mikrofrontend/$fødselsnummer"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].fødselsnummer").value(fødselsnummer))
            .andExpect(jsonPath("$[0].mikrofrontendId").value(MicrofrontendId.PLEIEPENGER_INNSYN.id))
            .andExpect(jsonPath("$[0].status").value("ENABLE"))
    }

    @Test
    fun `skal returnere tom liste når ingen mikrofrontender finnes`() {
        val fødselsnummer = "12345678901"

        every { mikrofrontendService.findByFødselsnummer(fødselsnummer) } returns emptyList()

        val token = tokenMedDriftsrolle(gyldigTestDriftRolle)

        mockMvc.perform(
            get(URI("/forvaltning/mikrofrontend/$fødselsnummer"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$").isEmpty)
    }

    @Test
    fun `skal aktivere mikrofrontend og kalle sendOgLagre`() {
        justRun { mikrofrontendService.sendOgLagre(any(), any()) }

        val request = MikrofrontendAktiverRequest(
            fødselsnummer = "12345678901",
        )

        val token = tokenMedDriftsrolle(gyldigTestDriftRolle)
        val requestBody = objectMapper.writeValueAsString(request)

        mockMvc.perform(
            post(URI("/forvaltning/mikrofrontend/aktiver"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.fødselsnummer").value("12345678901"))
            .andExpect(jsonPath("$.mikrofrontendId").value(MicrofrontendId.PLEIEPENGER_INNSYN.id))
            .andExpect(jsonPath("$.status").value("ENABLE"))

        verify(exactly = 1) { mikrofrontendService.sendOgLagre(any(), eq(MicrofrontendAction.ENABLE)) }
    }

    @Test
    fun `GET skal gi 403 dersom det mangler driftsrolle`() {
        val token = tokenMedDriftsrolle(ugyldigTestDriftRolle)

        mockMvc.perform(
            get(URI("/forvaltning/mikrofrontend/12345678901"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `POST aktiver skal gi 403 dersom det mangler driftsrolle`() {
        val request = MikrofrontendAktiverRequest(
            fødselsnummer = "12345678901",
        )

        val token = tokenMedDriftsrolle(ugyldigTestDriftRolle)
        val requestBody = objectMapper.writeValueAsString(request)

        mockMvc.perform(
            post(URI("/forvaltning/mikrofrontend/aktiver"))
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isForbidden)
    }

    private fun tokenMedDriftsrolle(driftRolle: String): String {
        return mockOAuth2Server.issueToken(
            ContextHolder.AZURE_AD,
            "theclientid",
            DefaultOAuth2TokenCallback(
                ContextHolder.AZURE_AD,
                "saksbehandler",
                JWT.type,
                listOf("aud-localhost"),
                defaultJwtClaimsSetBuilder()
                    .claim("NAVident", "TEST")
                    .claim("groups", arrayOf(driftRolle))
                    .build()
                    .claims,
                30L
            )
        ).serialize()
    }

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

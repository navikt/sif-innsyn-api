package no.nav.sifinnsynapi.forvaltning

import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.sifinnsynapi.config.Issuers
import no.nav.sifinnsynapi.dittnav.MicrofrontendAction
import no.nav.sifinnsynapi.dittnav.MicrofrontendId
import no.nav.sifinnsynapi.mikrofrontend.MikrofrontendDAO
import no.nav.sifinnsynapi.mikrofrontend.MikrofrontendService
import no.nav.sifinnsynapi.sikkerhet.AuthorizationService
import no.nav.sifinnsynapi.sikkerhet.ContextHolder
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.ErrorResponseException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.time.ZonedDateTime

@RestController
@RequiredIssuers(
    ProtectedWithClaims(issuer = Issuers.AZURE)
)
@Validated
class MikrofrontendForvaltningController(
    private val mikrofrontendService: MikrofrontendService,
    private val authorizationService: AuthorizationService
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(MikrofrontendForvaltningController::class.java)
    }

    @GetMapping(
        "/forvaltning/mikrofrontend/{fødselsnummer}",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ProtectedWithClaims(issuer = ContextHolder.AZURE_AD, claimMap = ["NAVident=*"])
    fun hentMikrofrontender(
        @PathVariable @Pattern(regexp = "\\d{11}", message = "Fødselsnummer må være 11 siffer") fødselsnummer: String
    ): ResponseEntity<List<MikrofrontendRespons>> {
        validerDriftsrolle()
        logger.info("Henter mikrofrontender for fødselsnummer")
        val mikrofrontender = mikrofrontendService.findByFødselsnummer(fødselsnummer)
        return ResponseEntity.ok(mikrofrontender.map { MikrofrontendRespons.fra(it) })
    }

    @PostMapping(
        "/forvaltning/mikrofrontend/aktiver",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ProtectedWithClaims(issuer = ContextHolder.AZURE_AD, claimMap = ["NAVident=*"])
    fun aktiverMikrofrontend(
        @Valid @RequestBody request: MikrofrontendAktiverRequest
    ): ResponseEntity<MikrofrontendRespons> {
        validerDriftsrolle()
        logger.info("Aktiverer mikrofrontend for fødselsnummer")

        val dao = MikrofrontendDAO(
            fødselsnummer = request.fødselsnummer,
            mikrofrontendId = MicrofrontendId.PLEIEPENGER_INNSYN.id,
            status = MicrofrontendAction.ENABLE,
            opprettet = ZonedDateTime.now(),
            behandlingsdato = null,
        )

        mikrofrontendService.sendOgLagre(dao, MicrofrontendAction.ENABLE)
        return ResponseEntity.status(HttpStatus.CREATED).body(MikrofrontendRespons.fra(dao))
    }

    private fun validerDriftsrolle() {
        if (!authorizationService.harTilgangTilDriftRolle()) {
            val problemDetail = ProblemDetail.forStatus(HttpStatus.FORBIDDEN)
            problemDetail.detail = "Mangler driftsrolle"
            throw ErrorResponseException(HttpStatus.FORBIDDEN, problemDetail, null)
        }
    }
}

package no.nav.sifinnsynapi.forvaltning

import jakarta.validation.Valid
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.sifinnsynapi.config.Issuers
import no.nav.sifinnsynapi.dittnav.MicrofrontendId
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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

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

    @PostMapping(
        "/forvaltning/mikrofrontend/hent",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ProtectedWithClaims(issuer = ContextHolder.AZURE_AD, claimMap = ["NAVident=*"])
    fun hentMikrofrontender(
        @Valid @RequestBody request: MikrofrontendOppslagRequest
    ): ResponseEntity<List<MikrofrontendRespons>> {
        validerDriftsrolle()
        logger.info("Henter mikrofrontender for fødselsnummer")
        val mikrofrontender = mikrofrontendService.findByFødselsnummer(request.fødselsnummer)
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
        val (dao, opprettet) = mikrofrontendService.aktiverIdempotent(
            fødselsnummer = request.fødselsnummer,
            mikrofrontendId = MicrofrontendId.PLEIEPENGER_INNSYN.id,
        )
        val httpStatus = if (opprettet) HttpStatus.CREATED else HttpStatus.OK
        return ResponseEntity.status(httpStatus).body(MikrofrontendRespons.fra(dao))
    }

    private fun validerDriftsrolle() {
        if (!authorizationService.harTilgangTilDriftRolle()) {
            val problemDetail = ProblemDetail.forStatus(HttpStatus.FORBIDDEN)
            problemDetail.detail = "Mangler driftsrolle"
            throw ErrorResponseException(HttpStatus.FORBIDDEN, problemDetail, null)
        }
    }
}

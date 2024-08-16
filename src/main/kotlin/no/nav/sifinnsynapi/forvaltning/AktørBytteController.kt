package no.nav.sifinnsynapi.forvaltning

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.sifinnsynapi.common.AktørId
import no.nav.sifinnsynapi.config.Issuers
import no.nav.sifinnsynapi.sikkerhet.AuthorizationService
import no.nav.sifinnsynapi.sikkerhet.ContextHolder
import no.nav.sifinnsynapi.soknad.SøknadService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.ErrorResponseException
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@RequiredIssuers(
    ProtectedWithClaims(issuer = Issuers.AZURE)
)
class AktørBytteController(
    private val søknadService: SøknadService,
    private val authorizationService: AuthorizationService
) {

    @PostMapping(
        "/forvaltning/oppdaterAktoerId",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @ProtectedWithClaims(issuer = ContextHolder.AZURE_AD, claimMap = ["NAVident=*"])
    fun oppdaterAktoerId(@RequestBody aktørBytteRequest: AktørBytteRequest): ResponseEntity<AktørBytteRespons> {
        if (!authorizationService.harTilgangTilDriftRolle()) {
            val problemDetail = ProblemDetail.forStatus(HttpStatus.FORBIDDEN)
            problemDetail.detail = "Mangler driftsrolle"
            throw ErrorResponseException(HttpStatus.FORBIDDEN, problemDetail, null)
        }
        val antallOppdaterte = søknadService.oppdaterAktørId(
            AktørId(aktørBytteRequest.gyldigAktør),
            AktørId(aktørBytteRequest.utgåttAktør)
        )
        return ResponseEntity.ok(AktørBytteRespons(antallOppdaterte))
    }

}

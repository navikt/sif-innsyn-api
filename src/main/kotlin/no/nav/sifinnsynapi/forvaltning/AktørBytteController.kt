package no.nav.sifinnsynapi.forvaltning

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.sifinnsynapi.common.AktørId
import no.nav.sifinnsynapi.config.Issuers
import no.nav.sifinnsynapi.soknad.SøknadService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@RequiredIssuers(
    ProtectedWithClaims(issuer = Issuers.TOKEN_X, claimMap = ["acr=Level4"])
)
class AktørBytteController(
    private val søknadService: SøknadService
) {

    @PostMapping("forvaltning/oppdaterAktoerId",
        produces = [MediaType.APPLICATION_JSON_VALUE])
    fun oppdaterAktoerId(@RequestBody aktørBytteRequest: AktørBytteRequest): ResponseEntity<AktørBytteRespons> {
        val antallOppdaterte = søknadService.oppdaterAktørId(
            AktørId(aktørBytteRequest.gyldigAktør),
            AktørId(aktørBytteRequest.utgåttAktør)
        )
        return ResponseEntity.ok(AktørBytteRespons(antallOppdaterte))
    }


}

package no.nav.sifinnsynapi.k9sakinnsynapi

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.sifinnsynapi.Routes.K9_SAK_INNSYN
import no.nav.sifinnsynapi.config.Issuers
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@RestController
@RequiredIssuers(
    ProtectedWithClaims(issuer = Issuers.TOKEN_X, claimMap = ["acr=Level4"])
)
class K9SakInnsynSøknadController(
        private val k9SakInnsynApiService: K9SakInnsynApiService
) {
    companion object {
        val logger = LoggerFactory.getLogger(K9SakInnsynSøknadController::class.java)
    }

    @GetMapping(K9_SAK_INNSYN, produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseStatus(OK)
    fun hentSøknader(): List<K9SakInnsynSøknad> {
        logger.info("Henter innsyn i søknadsopplysninger...")
        val søknader = k9SakInnsynApiService.hentSøknadsopplysninger()
        return søknader
    }
}

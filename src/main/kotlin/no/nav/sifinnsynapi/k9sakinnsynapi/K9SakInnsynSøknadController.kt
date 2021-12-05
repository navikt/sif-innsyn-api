package no.nav.sifinnsynapi.k9sakinnsynapi

import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sifinnsynapi.Routes.K9_SAK_INNSYN
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

@RestController
@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
class K9SakInnsynSøknadController(
        private val k9SakInnsynApiService: K9SakInnsynApiService
) {
    companion object {
        val logger = LoggerFactory.getLogger(K9SakInnsynSøknadController::class.java)
    }

    @GetMapping(K9_SAK_INNSYN, produces = [MediaType.APPLICATION_JSON_VALUE])
    @Protected
    @ResponseStatus(OK)
    fun hentSøknader(): List<K9SakInnsynSøknad> {
        logger.info("Henter innsyn i søknadsopplysninger...")
        val søknader = k9SakInnsynApiService.hentSøknadsopplysninger()
        return søknader
    }
}

package no.nav.sifinnsynapi.poc

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
class PocController(
        private val søknadService: SøknadService
) {
    companion object {
        val logger = LoggerFactory.getLogger(PocController::class.java)
    }

    @GetMapping("/soknad",  produces = [MediaType.APPLICATION_JSON_VALUE])
    @Unprotected
    fun hentSøknad(): List<SøknadDTO> {
        logger.info("henter søknader...")
        return søknadService.hentSøknad()
    }
}

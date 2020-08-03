package no.nav.sifinnsynapi.soknad

import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController


@RestController
@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
class SøknadController(
        private val søknadService: SøknadService
) {
    companion object {
        val logger = LoggerFactory.getLogger(SøknadController::class.java)
    }

    @GetMapping("/soknad", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Protected
    @ResponseStatus(OK)
    fun hentSøknad(): List<SøknadDTO> {
        logger.info("Forsøker å hente søknader...")
        val søknader = søknadService.hentSøknad()
        logger.info("Fant {} søknader", søknader.size)
        return søknader
    }
}

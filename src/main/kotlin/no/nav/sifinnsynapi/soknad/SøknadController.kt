package no.nav.sifinnsynapi.soknad

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.RequiredIssuers
import no.nav.sifinnsynapi.Routes.SØKNAD
import no.nav.sifinnsynapi.common.Søknadstype
import no.nav.sifinnsynapi.config.Issuers
import org.slf4j.LoggerFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequiredIssuers(
    ProtectedWithClaims(issuer = Issuers.ID_PORTEN, claimMap = ["acr=Level4"]),
    ProtectedWithClaims(issuer = Issuers.TOKEN_X, claimMap = ["acr=Level4"])
)
class SøknadController(
        private val søknadService: SøknadService
) {
    companion object {
        val logger = LoggerFactory.getLogger(SøknadController::class.java)
    }

    @GetMapping(SØKNAD, produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseStatus(OK)
    fun hentSøknader(): List<SøknadDTO> {
        logger.info("Forsøker å hente søknader...")
        val søknader = søknadService.hentSøknader()
        logger.info("Fant {} søknader", søknader.size)
        return søknader
    }

    @GetMapping("$SØKNAD/{søknadId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseStatus(OK)
    fun hentSøknad(@PathVariable søknadId: UUID): SøknadDTO {
        logger.info("Forsøker å hente søknad med id : {}...", søknadId)
        return søknadService.hentSøknad(søknadId)
    }

    @GetMapping("$SØKNAD/psb/siste", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseStatus(OK)
    fun hentSistInnsendtePSBSøknad(): SøknadDTO {
        logger.info("Forsøker å hente sist innsendte søknad...")
        val søknadstype = Søknadstype.PP_SYKT_BARN
        return søknadService.hentSistInnsendteSøknad(søknadstype) ?: throw SøknadNotFoundException(søknadstype)
    }

    @GetMapping("$SØKNAD/{søknadId}/arbeidsgivermelding", produces = [MediaType.APPLICATION_PDF_VALUE])
    @ResponseStatus(OK)
    fun lastNedArbeidsgivermelding(
        @PathVariable søknadId: UUID,
        @RequestParam organisasjonsnummer: String
    ): ResponseEntity<Resource> {
        val resource = ByteArrayResource(søknadService.hentArbeidsgiverMeldingFil(søknadId, organisasjonsnummer))

        val filnavn = "Bekreftelse_til_arbeidsgiver_$organisasjonsnummer"

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=$filnavn.pdf")
                .contentLength(resource.byteArray.size.toLong())
                .body(resource)
    }
}

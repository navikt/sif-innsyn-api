package no.nav.sifinnsynapi.soknad

import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sifinnsynapi.Routes.SØKNAD
import no.nav.sifinnsynapi.dokument.DokumentService
import org.slf4j.LoggerFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.*


@RestController
@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
class SøknadController(
        private val søknadService: SøknadService,
        private val dokumentService: DokumentService
) {
    companion object {
        val logger = LoggerFactory.getLogger(SøknadController::class.java)
    }

    @GetMapping(SØKNAD, produces = [MediaType.APPLICATION_JSON_VALUE])
    @Protected
    @ResponseStatus(OK)
    fun hentSøknader(): List<SøknadDTO> {
        logger.info("Forsøker å hente søknader...")
        val søknader = søknadService.hentSøknader()
        logger.info("Fant {} søknader", søknader.size)
        return søknader
    }

    @GetMapping("$SØKNAD/{søknadId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Protected
    @ResponseStatus(OK)
    fun hentSøknad(@PathVariable søknadId: UUID): SøknadDTO {
        logger.info("Forsøker å hente søknad med id : {}...", søknadId)
        return søknadService.hentSøknad(søknadId)
    }

    @GetMapping("$SØKNAD/{søknadId}/dokument", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    @Protected
    @ResponseStatus(OK)
    fun hentDokument(@PathVariable søknadId: UUID): ResponseEntity<Resource> {
        logger.info("Forsøker å hente dokument for søknad med id : {}...", søknadId)
        val dokumentDAO = dokumentService.hentDokument(søknadId)
        val resource = ByteArrayResource(dokumentDAO!!.innhold)

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "filename=søknad.pdf")
                .contentLength(dokumentDAO.innhold.size.toLong())
                .body(resource)

    }
}
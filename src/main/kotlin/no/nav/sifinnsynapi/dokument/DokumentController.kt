package no.nav.sifinnsynapi.dokument

import no.nav.security.token.support.core.api.Protected
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.sifinnsynapi.Routes.DOKUMENT
import no.nav.sifinnsynapi.safselvbetjening.ArkivertDokument
import no.nav.sifinnsynapi.safselvbetjening.generated.hentdokumentoversikt.Dokumentoversikt
import org.slf4j.LoggerFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@ProtectedWithClaims(issuer = "selvbetjening", claimMap = ["acr=Level4"])
class DokumentController(
    private val dokumentService: DokumentService
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(DokumentController::class.java)
    }

    @GetMapping("$DOKUMENT/oversikt", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Protected
    @ResponseStatus(HttpStatus.OK)
    fun hentDokumentOversikt(
        @RequestParam vararg brevkoder: String
    ): Dokumentoversikt {
        logger.info("Henter dokumentoversikt...")
        return dokumentService.hentDokumentOversikt(brevkoder.asList())
    }

    @GetMapping(
        "$DOKUMENT/{journalpostId}/{dokumentInfoId}/{variantFormat}",
        produces = [MediaType.APPLICATION_PDF_VALUE]
    )
    @Protected
    @ResponseStatus(HttpStatus.OK)
    fun hentDokument(
        @PathVariable journalpostId: String,
        @PathVariable dokumentInfoId: String,
        @PathVariable variantFormat: String,
        @RequestParam dokumentTittel: String
    ): ResponseEntity<Resource> {
        logger.info("Henter dokument for journalpostId: {}", journalpostId)

        val dokument = dokumentService.hentDokument(journalpostId, dokumentInfoId, variantFormat)
        val resource = ByteArrayResource(dokument.body)

        val formatertFilnavn = filnavn(dokumentTittel)

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=$formatertFilnavn")
            .contentLength(resource.byteArray.size.toLong())
            .body(resource)
    }
}

/**
 * Erstatter filtype på dokumentTittel med contentType fra hentet dokument.
 *
 */
fun filnavn(dokumentTittel: String): String =
    dokumentTittel.replaceAfterLast(".", "pdf")

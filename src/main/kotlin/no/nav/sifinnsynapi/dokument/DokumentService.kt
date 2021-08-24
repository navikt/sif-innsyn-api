package no.nav.sifinnsynapi.dokument

import kotlinx.coroutines.runBlocking
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.sifinnsynapi.safselvbetjening.ArkivertDokument
import no.nav.sifinnsynapi.safselvbetjening.SafSelvbetjeningService
import no.nav.sifinnsynapi.safselvbetjening.generated.hentdokumentoversikt.DokumentInfo
import no.nav.sifinnsynapi.safselvbetjening.generated.hentdokumentoversikt.Dokumentoversikt
import no.nav.sifinnsynapi.safselvbetjening.generated.hentdokumentoversikt.Journalpost
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DokumentService(
    private val safSelvbetjeningService: SafSelvbetjeningService,
    private val tokenValidationContextHolder: SpringTokenValidationContextHolder
) {
    private companion object {
        private val logger = LoggerFactory.getLogger(DokumentService::class.java)
    }

    fun hentDokumentOversikt(brevkoder: List<String>): Dokumentoversikt = runBlocking {
        val token = tokenValidationContextHolder.tokenValidationContext.firstValidToken.get()
        val dokumentoversikt = safSelvbetjeningService.hentDokumentoversikt(token.subject)
        logger.info("Filtererer på brevkoder: {}", brevkoder)

        val filtererteJournalposter: List<Journalpost> =
            dokumentoversikt.journalposter.filter { journalpost: Journalpost ->
                journalpost.dokumenter!!.any { dokumentInfo: DokumentInfo? -> brevkoder.contains(dokumentInfo!!.brevkode!!.lowercase().trim()) }
            }
        dokumentoversikt.copy(journalposter = filtererteJournalposter)
    }

    fun hentDokument(journalpostId: String, dokumentInfoId: String, varianFormat: String): ArkivertDokument {
        return safSelvbetjeningService.hentDokument(journalpostId, dokumentInfoId, varianFormat)
    }

}

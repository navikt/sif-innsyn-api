package no.nav.sifinnsynapi.dokument

import kotlinx.coroutines.runBlocking
import no.nav.sifinnsynapi.common.Søknadstype
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

    ) {

    companion object {
        private val logger = LoggerFactory.getLogger(DokumentService::class.java)
        private val fellesBrevkoder = listOf("INNVILGELSE", "INNHEN", "HENLEG", "INNLYS")
        val brevkoder = mapOf(
            Søknadstype.PP_SYKT_BARN to listOf("NAV 09-11.05") + fellesBrevkoder
        )
    }

    fun hentDokumentOversikt(brevkoder: List<String>): Dokumentoversikt = runBlocking {
        safSelvbetjeningService.hentDokumentoversikt()
            .medRelevanteBrevkoder(brevkoder)
    }

    fun hentDokument(journalpostId: String, dokumentInfoId: String, varianFormat: String): ArkivertDokument {
        return safSelvbetjeningService.hentDokument(journalpostId, dokumentInfoId, varianFormat)
    }

    fun Dokumentoversikt.medRelevanteBrevkoder(brevkoder: List<String>): Dokumentoversikt {
        logger.info("Filtrerer dokumentoversikt på følgende brevkoder: {} ...", brevkoder)
        return Dokumentoversikt(
            journalposter = journalposter.filter { journalpost: Journalpost ->
                journalpost.dokumenter!!.harRelevantBrevkode(brevkoder)
            }
        )
    }

    fun List<DokumentInfo?>.harRelevantBrevkode(brevkoder: List<String>): Boolean =
        any { dokumentInfo: DokumentInfo? ->
            brevkoder.map { it.lowercase().trim() }
                .contains(dokumentInfo!!.brevkode!!.lowercase().trim())
        }
}

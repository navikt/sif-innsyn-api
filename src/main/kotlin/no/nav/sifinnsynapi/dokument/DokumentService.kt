package no.nav.sifinnsynapi.dokument

import kotlinx.coroutines.runBlocking
import no.nav.sifinnsynapi.Routes
import no.nav.sifinnsynapi.common.Søknadstype
import no.nav.sifinnsynapi.safselvbetjening.ArkivertDokument
import no.nav.sifinnsynapi.safselvbetjening.SafSelvbetjeningService
import no.nav.sifinnsynapi.safselvbetjening.generated.enums.Variantformat
import no.nav.sifinnsynapi.safselvbetjening.generated.hentdokumentoversikt.DokumentInfo
import no.nav.sifinnsynapi.safselvbetjening.generated.hentdokumentoversikt.Dokumentoversikt
import no.nav.sifinnsynapi.safselvbetjening.generated.hentdokumentoversikt.Journalpost
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URL

@Service
class DokumentService(
    private val safSelvbetjeningService: SafSelvbetjeningService,
    @Value("\${application-ingress}") val applicationIngress: String
) {

    companion object {
        private val logger = LoggerFactory.getLogger(DokumentService::class.java)
        private val fellesBrevkoder = listOf("INNVILGELSE", "INNHEN", "HENLEG", "INNLYS")
        val brevkoder = mapOf(
            Søknadstype.PP_SYKT_BARN to listOf("NAV 09-11.05") + fellesBrevkoder
        )
    }

    fun hentDokumentOversikt(brevkoder: List<String>): List<DokumentDTO> = runBlocking {
        safSelvbetjeningService.hentDokumentoversikt()
            .medRelevanteBrevkoder(brevkoder)
            .somDokumentDTO(applicationIngress)
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

private fun Dokumentoversikt.somDokumentDTO(applicationIngress: String): List<DokumentDTO> =
    journalposter.flatMap { journalpost ->
        journalpost.dokumenter!!.map { dokumentInfo: DokumentInfo? ->
            val journalpostId = journalpost.journalpostId
            val sakId = journalpost.sak?.fagsakId
            val relevanteDatoer = journalpost.relevanteDatoer.map { it!! }

            val dokumentInfoId = dokumentInfo!!.dokumentInfoId
            val dokumentvariant = dokumentInfo.dokumentvarianter.first { it!!.variantformat == Variantformat.ARKIV }!!
            val brukerHarTilgang = dokumentvariant.brukerHarTilgang
            val tittel = dokumentInfo.tittel!!

            DokumentDTO(
                journalpostId = journalpostId,
                sakId = sakId,
                dokumentInfoId = dokumentInfoId,
                tittel = tittel,
                harTilgang = brukerHarTilgang,
                url = URL("$applicationIngress${Routes.DOKUMENT}/$journalpostId/$dokumentInfoId/${dokumentvariant.variantformat.name}"),
                relevanteDatoer = relevanteDatoer
            )
        }
    }

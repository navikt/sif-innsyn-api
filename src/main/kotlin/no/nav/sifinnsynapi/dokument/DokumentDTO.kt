package no.nav.sifinnsynapi.dokument

import no.nav.sifinnsynapi.safselvbetjening.generated.hentdokumentoversikt.RelevantDato
import java.net.URL

data class DokumentDTO(
    val journalpostId: String,
    val dokumentInfoId: String,
    val sakId: String?,
    val tittel: String,
    val harTilgang: Boolean,
    val url: URL,
    val relevanteDatoer: List<RelevantDato>
)

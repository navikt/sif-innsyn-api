package no.nav.sifinnsynapi.common

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class SøknadsHendelse @JsonCreator constructor(
        @JsonProperty("metadata") val metadata: Metadata,
        @JsonAlias("meldingV1") @JsonProperty("melding") val melding: Map<String, Any>,
        @JsonProperty("pdfDokument") val pdfDokument: ByteArray? =  null,
        @JsonProperty("journalførtMelding") val journalførtMelding: JournalfortMelding
)

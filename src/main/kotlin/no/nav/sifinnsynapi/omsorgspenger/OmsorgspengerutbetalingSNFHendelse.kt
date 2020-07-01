package no.nav.sifinnsynapi.omsorgspenger

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonRootName
import no.nav.sifinnsynapi.common.JournalfortMelding
import no.nav.sifinnsynapi.common.Metadata

data class OmsorgspengerutbetalingSNFHendelse @JsonCreator constructor(
        @JsonProperty("metadata") val metadata: Metadata,
        @JsonProperty("melding") val melding: Map<String, Any>,
        @JsonProperty("journalførtMelding") val journalførtMelding: JournalfortMelding
)
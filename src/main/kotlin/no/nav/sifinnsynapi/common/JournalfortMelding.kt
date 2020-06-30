package no.nav.sifinnsynapi.common

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class JournalfortMelding @JsonCreator constructor(
        @JsonProperty("journalpostId") val journalpostId: String
)
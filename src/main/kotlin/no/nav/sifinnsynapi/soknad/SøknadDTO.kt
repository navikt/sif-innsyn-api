package no.nav.sifinnsynapi.soknad

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.sifinnsynapi.common.SøknadsStatus
import no.nav.sifinnsynapi.common.Søknadstype
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

data class SøknadDTO @JsonCreator constructor(
        @JsonProperty("søknadId") val søknadId: UUID,
        @JsonProperty("søknadstype") val søknadstype: Søknadstype,
        @JsonProperty("status") val status: SøknadsStatus,
        @JsonProperty("søknad") val søknad: Map<String, Any>,
        @JsonProperty("saksId") val saksId: String?,
        @JsonProperty("journalpostId") val journalpostId: String,
        @JsonProperty("opprettet") @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "Europe/Oslo") val opprettet: ZonedDateTime? = null,
        @JsonProperty("endret") @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") val endret: LocalDateTime? = null,
        @JsonProperty("behandlingsdato") @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") val behandlingsdato: LocalDate? = null
)

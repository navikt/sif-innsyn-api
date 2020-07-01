package no.nav.sifinnsynapi.poc

import com.fasterxml.jackson.annotation.JsonFormat
import no.nav.sifinnsynapi.common.SøknadsStatus
import no.nav.sifinnsynapi.common.Søknadstype
import java.time.LocalDate
import java.time.LocalDateTime

data class SøknadDTO(
        val søknadstype: Søknadstype,
        val status: SøknadsStatus,
        val søknad: Map<String, Any>,
        val saksId: String?,
        val journalpostId: String,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") val opprettet: LocalDateTime? = null,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") val endret: LocalDateTime? = null,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss") val behandlingsdato: LocalDate? = null
)

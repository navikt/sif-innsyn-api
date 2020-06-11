package no.nav.sifinnsynapi.poc

import no.nav.sifinnsynapi.common.SøknadsStatus
import no.nav.sifinnsynapi.common.Søknadstype
import java.time.LocalDate
import java.time.LocalDateTime

data class SøknadDTO(
        val søknadstype: Søknadstype,
        val status: SøknadsStatus,
        val søknad: Map<String, Any>,
        val saksId: String,
        val journalpostId: String,
        val opprettet: LocalDateTime? = null,
        val endret: LocalDateTime? = null,
        val behandlingsdato: LocalDate? = null
)

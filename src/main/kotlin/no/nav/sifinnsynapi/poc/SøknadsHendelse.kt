package no.nav.sifinnsynapi.poc

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.sifinnsynapi.common.*
import org.springframework.lang.Nullable
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import javax.validation.constraints.NotNull

data class SøknadsHendelse @JsonCreator constructor(
        @JsonProperty("aktørId") val aktørId: AktørId,
        @JsonProperty("fødselsnummer") val fnr: Fødselsnummer,
        @JsonProperty("journalpostId") val journalId: String,
        @JsonProperty("saksnummer") var saksnummer: String?,
        @JsonProperty("status") val status: SøknadsStatus,
        @JsonProperty("søknadstype") val søknadstype: Søknadstype,
        @JsonProperty("førsteBehandlingsdato") var førsteBehandlingsdato: LocalDate?,
        @JsonProperty("mottattDato") val mottattDato: LocalDateTime) {

    override fun toString(): String {
        return "SøknadsHendelse(aktørId=$aktørId, fnr=$fnr, journalId='$journalId', saksnummer=$saksnummer, " +
                "status=$status, søknadstype=$søknadstype, førsteBehandlingsdato=$førsteBehandlingsdato, mottattDato=$mottattDato)"
    }
}
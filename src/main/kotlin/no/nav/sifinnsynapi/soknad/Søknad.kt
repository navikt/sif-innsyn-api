package no.nav.sifinnsynapi.soknad

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.sifinnsynapi.common.AktørId
import no.nav.sifinnsynapi.common.Fødselsnummer
import no.nav.sifinnsynapi.common.SøknadsStatus
import no.nav.sifinnsynapi.common.Søknadstype
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.*

data class Søknad @JsonCreator constructor(
        @JsonProperty("aktørId") val aktørId: AktørId,
        @JsonProperty("fødselsnummer") val fødselsnummer: Fødselsnummer,
        @JsonProperty("journalpostId") val journalpostId: String,
        @JsonProperty("saksnummer") var saksnummer: String? = null,
        @JsonProperty("status") val status: SøknadsStatus,
        @JsonProperty("søknadstype") val søknadstype: Søknadstype,
        @JsonProperty("førsteBehandlingsdato") var førsteBehandlingsdato: LocalDate? = null,
        @JsonProperty("mottattDato") @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX", timezone = "UTC") val mottattDato: ZonedDateTime,
        @JsonProperty("søknad") val søknad: Map<String, Any>) {

    override fun toString(): String {
        return "SøknadsHendelse(aktørId=$aktørId, fødselsnummer=$fødselsnummer, journalpostId='$journalpostId', saksnummer=$saksnummer, " +
                "status=$status, søknadstype=$søknadstype, førsteBehandlingsdato=$førsteBehandlingsdato, mottattDato=$mottattDato)"
    }
}

package no.nav.sifinnsynapi.poc

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.sifinnsynapi.common.*
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZonedDateTime

data class SøknadsHendelse @JsonCreator constructor(
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

    fun tilSøknadDAO(): SøknadDAO = SøknadDAO(
            aktørId = aktørId,
            saksId = saksnummer,
            fødselsnummer = fødselsnummer,
            journalpostId = journalpostId,
            søknad = JSONObject(søknad).toString(),
            status = status,
            søknadstype = søknadstype,
            behandlingsdato = førsteBehandlingsdato,
            opprettet = mottattDato.toLocalDateTime(),
            endret = null
    )

}

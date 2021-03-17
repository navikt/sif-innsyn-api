package no.nav.sifinnsynapi.pleiepenger.syktbarn

import no.nav.sifinnsynapi.http.PleiepengesøknadMedOrganisasjonsnummerIkkeFunnetException
import no.nav.sifinnsynapi.soknad.SøknadDAO
import org.json.JSONObject
import java.time.LocalDate

internal object PleiepengersøknadKeysV1 {
    const val SØKNAD_ID = "søknadId"
    const val SØKER = "søker"
    const val SØKER_FORNAVN = "fornavn"
    const val SØKER_MELLOMNAVN = "mellomnavn"
    const val SØKER_ETTERNAVN = "etternavn"
    const val AKTØR_ID = "aktørId"
    const val MOTTATT = "mottatt"
    const val FØDSELSNUMMER = "fødselsnummer"
    const val FRA_OG_MED = "fraOgMed"
    const val TIL_OG_MED = "tilOgMed"
    const val ORGANISASJONSNAVN = "navn"
    const val ORGANISASJONSNUMMER = "organisasjonsnummer"
    const val ARBEIDSGIVERE = "arbeidsgivere"
    const val ORGANISASJONER = "organisasjoner"
}

internal object PleiepengerJSONObjectUtils {
    fun JSONObject.finnOrganisasjon(søknad: SøknadDAO, organisasjonsnummer: String): JSONObject {
        val organisasjoner = getJSONObject(PleiepengersøknadKeysV1.ARBEIDSGIVERE).getJSONArray(PleiepengersøknadKeysV1.ORGANISASJONER)
        var funnetOrg: JSONObject? = null

        for (i in 0 until organisasjoner.length()) {
            val org = organisasjoner.getJSONObject(i)
            if (org.getString(PleiepengersøknadKeysV1.ORGANISASJONSNUMMER) == organisasjonsnummer) {
                funnetOrg = org
            }
        }

        if (funnetOrg == null) throw PleiepengesøknadMedOrganisasjonsnummerIkkeFunnetException(
            søknad.id.toString(),
            organisasjonsnummer
        )
        return funnetOrg
    }

    fun JSONObject.tilArbeidstakernavn(): String = when(optString(PleiepengersøknadKeysV1.SØKER_MELLOMNAVN, null)) {
        null -> "${getString(PleiepengersøknadKeysV1.SØKER_FORNAVN)} ${getString(PleiepengersøknadKeysV1.SØKER_ETTERNAVN)}"
        else -> "${getString(PleiepengersøknadKeysV1.SØKER_FORNAVN)} ${getString(PleiepengersøknadKeysV1.SØKER_MELLOMNAVN)} ${getString(
            PleiepengersøknadKeysV1.SØKER_ETTERNAVN
        )}"
    }


    fun JSONObject.tilPleiepengerAreidsgivermelding(funnetOrg: JSONObject) = PleiepengerArbeidsgiverMelding(
        søknadsperiode = SøknadsPeriode(
            fraOgMed = LocalDate.parse(getString(PleiepengersøknadKeysV1.FRA_OG_MED)),
            tilOgMed = LocalDate.parse(getString(PleiepengersøknadKeysV1.TIL_OG_MED)),
        ),
        arbeidsgivernavn = funnetOrg.optString(PleiepengersøknadKeysV1.ORGANISASJONSNAVN, null),
        arbeidstakernavn = getJSONObject(PleiepengersøknadKeysV1.SØKER).tilArbeidstakernavn()
    )
}

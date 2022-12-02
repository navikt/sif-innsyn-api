package no.nav.sifinnsynapi.konsument.pleiepenger.syktbarn

import no.nav.sifinnsynapi.konsument.pleiepenger.syktbarn.PleiepengersøknadKeysV1.ARBEIDSGIVERE
import no.nav.sifinnsynapi.soknad.SøknadDAO
import no.nav.sifinnsynapi.util.ServletUtils
import org.json.JSONArray
import org.json.JSONObject
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.ErrorResponseException
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.Charset
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
        val organisasjoner = when (val arbeidsgivereObjekt = get(ARBEIDSGIVERE)) {
            is JSONObject -> arbeidsgivereObjekt.getJSONArray(PleiepengersøknadKeysV1.ORGANISASJONER)
            is JSONArray -> arbeidsgivereObjekt
            else -> throw Error("Ugyldig type for feltet $ARBEIDSGIVERE. Forventet enten JSONObject eller JSONArray, men fikk ${arbeidsgivereObjekt.javaClass}")
        }

        var organisasjon: JSONObject? = null

        for (i in 0 until organisasjoner.length()) {
            val org = organisasjoner.getJSONObject(i)
            if (org.getString(PleiepengersøknadKeysV1.ORGANISASJONSNUMMER) == organisasjonsnummer) {
                organisasjon = org
            }
        }

        if (organisasjon == null) throw PleiepengesøknadMedOrganisasjonsnummerIkkeFunnetException(
            søknad.id.toString(),
            organisasjonsnummer
        )
        return organisasjon
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

class PleiepengesøknadMedOrganisasjonsnummerIkkeFunnetException(søknadId: String, organisasjonsnummer: String) :
    ErrorResponseException(HttpStatus.NOT_FOUND, asProblemDetail(søknadId, organisasjonsnummer), null) {
    private companion object {
        private fun asProblemDetail(søknadId: String, organisasjonsnummer: String): ProblemDetail {
            val problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND)
            problemDetail.title = "Arbeidsgiver ikke funnet"
            problemDetail.detail =
                "Søknad med søknadId = $søknadId  og organisasjonsnummer = $organisasjonsnummer ble ikke funnet."
            problemDetail.type = URI("/problem-details/arbeidsgiver-ikke-funnet")
            ServletUtils.currentHttpRequest()?.let {
                problemDetail.instance = URI(URLDecoder.decode(it.requestURL.toString(), Charset.defaultCharset()))
            }

            return problemDetail
        }
    }
}

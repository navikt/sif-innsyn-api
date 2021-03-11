package no.nav.sifinnsynapi.soknad

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.sifinnsynapi.common.AktørId
import no.nav.sifinnsynapi.common.Søknadstype
import no.nav.sifinnsynapi.http.NotSupportedArbeidsgiverMeldingException
import no.nav.sifinnsynapi.http.PleiepengesøknadMedOrganisasjonsnummerIkkeFunnetException
import no.nav.sifinnsynapi.http.SøknadNotFoundException
import no.nav.sifinnsynapi.oppslag.OppslagsService
import no.nav.sifinnsynapi.pleiepenger.syktbarn.PdfV1GeneratorService
import org.json.JSONObject
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

@Service
class SøknadService(
    private val repo: SøknadRepository,
    private val oppslagsService: OppslagsService,
    private val pdfV1GeneratorService: PdfV1GeneratorService
) {

    companion object {
        private val mapper = ObjectMapper()
    }

    fun hentSøknader(): List<SøknadDTO> {

        val aktørId = AktørId.valueOf(oppslagsService.hentAktørId()!!.aktør_id)

        return repo.findAllByAktørId(aktørId).map {
            it.tilSøknadDTO()
        }
    }

    fun hentSøknad(søknadId: UUID): SøknadDTO {
        return repo.findById(søknadId).orElseThrow {
            SøknadNotFoundException(søknadId.toString())
        }.tilSøknadDTO()
    }

    fun SøknadDAO.tilSøknadDTO() = SøknadDTO(
        søknadId = id,
        saksId = saksId,
        journalpostId = journalpostId,
        søknadstype = søknadstype,
        status = status,
        opprettet = opprettet,
        endret = endret,
        behandlingsdato = behandlingsdato,
        søknad = mapper.readValue(
            søknad,
            object :
                TypeReference<MutableMap<String, Any>>() {}
        )
    )

    fun hentArbeidsgiverMeldingFil(søknadId: UUID, organisasjonsnummer: String): ByteArray {

        val søknad = repo.findById(søknadId).orElseThrow {
            SøknadNotFoundException(søknadId.toString())
        }
        return when (søknad.søknadstype) {
            Søknadstype.PP_SYKT_BARN -> {
                val json = JSONObject(søknad.søknad)
                val organisasjoner = json.getJSONObject("arbeidsgivere").getJSONArray("organisasjoner")
                var funnetOrg: JSONObject? = null

                for (i in 0 until organisasjoner.length()) {
                    val org = organisasjoner.getJSONObject(i)
                    if (org.getString("organisasjonsnummer") == organisasjonsnummer) {
                        funnetOrg = org
                    }
                }

                if (funnetOrg == null) throw PleiepengesøknadMedOrganisasjonsnummerIkkeFunnetException(
                    søknad.id.toString(),
                    organisasjonsnummer
                )

                pdfV1GeneratorService.generateSoknadOppsummeringPdf(PleiepengerArbeidsgiverMelding(
                    søknadsperiode = SøknadsPeriode(
                        fraOgMed = LocalDate.parse(json.getString("fraOgMed")),
                        tilOgMed = LocalDate.parse(json.getString("tilOgMed")),
                    ),
                    arbeidsgivernavn = funnetOrg.optString("navn", null),
                    arbeidstakernavn = json.getJSONObject("søker").tilArbeidstakernavn()
                ))
            }

            else -> throw NotSupportedArbeidsgiverMeldingException(søknad.id.toString(), søknad.søknadstype)
        }
    }
}

private fun JSONObject.tilArbeidstakernavn(): String = when(optString("mellomnavn", null)) {
    null -> "${getString("fornavn")} ${getString("etternavn")}"
    else -> "${getString("fornavn")} ${getString("mellomnavn")} ${getString("etternavn")}"
}

data class PleiepengerArbeidsgiverMelding(
    val arbeidstakernavn: String,
    val arbeidsgivernavn: String? = null,
    val søknadsperiode: SøknadsPeriode
)

data class SøknadsPeriode(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate
)

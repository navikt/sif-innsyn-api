package no.nav.sifinnsynapi.soknad

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.sifinnsynapi.common.AktørId
import no.nav.sifinnsynapi.common.Søknadstype
import no.nav.sifinnsynapi.http.NotSupportedArbeidsgiverMeldingException
import no.nav.sifinnsynapi.http.SøknadNotFoundException
import no.nav.sifinnsynapi.oppslag.OppslagsService
import no.nav.sifinnsynapi.pleiepenger.syktbarn.ArbeidsgiverMeldingPDFGenerator
import no.nav.sifinnsynapi.pleiepenger.syktbarn.PleiepengerJSONObjectUtils.finnOrganisasjon
import no.nav.sifinnsynapi.pleiepenger.syktbarn.PleiepengerJSONObjectUtils.tilPleiepengerAreidsgivermelding
import org.json.JSONObject
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

@Service
class SøknadService(
    private val repo: SøknadRepository,
    private val oppslagsService: OppslagsService,
    private val arbeidsgiverMeldingPDFGenerator: ArbeidsgiverMeldingPDFGenerator
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
                val pleiepengesøknadJson = JSONObject(søknad.søknad)
                val funnetOrg: JSONObject = pleiepengesøknadJson.finnOrganisasjon(søknad, organisasjonsnummer)

                arbeidsgiverMeldingPDFGenerator.genererPDF(pleiepengesøknadJson.tilPleiepengerAreidsgivermelding(funnetOrg))
            }

            else -> throw NotSupportedArbeidsgiverMeldingException(søknad.id.toString(), søknad.søknadstype)
        }
    }
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

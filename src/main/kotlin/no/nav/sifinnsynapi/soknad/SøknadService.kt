package no.nav.sifinnsynapi.soknad

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.sifinnsynapi.Routes
import no.nav.sifinnsynapi.common.AktørId
import no.nav.sifinnsynapi.common.Søknadstype
import no.nav.sifinnsynapi.dokument.DokumentDTO
import no.nav.sifinnsynapi.dokument.DokumentService
import no.nav.sifinnsynapi.dokument.DokumentService.Companion.brevkoder
import no.nav.sifinnsynapi.http.NotSupportedArbeidsgiverMeldingException
import no.nav.sifinnsynapi.http.SøknadNotFoundException
import no.nav.sifinnsynapi.http.SøknadWithJournalpostIdNotFoundException
import no.nav.sifinnsynapi.konsument.pleiepenger.syktbarn.ArbeidsgiverMeldingPDFGenerator
import no.nav.sifinnsynapi.konsument.pleiepenger.syktbarn.PleiepengerJSONObjectUtils.finnOrganisasjon
import no.nav.sifinnsynapi.konsument.pleiepenger.syktbarn.PleiepengerJSONObjectUtils.tilPleiepengerAreidsgivermelding
import no.nav.sifinnsynapi.oppslag.OppslagsService
import no.nav.sifinnsynapi.safselvbetjening.generated.enums.Variantformat
import no.nav.sifinnsynapi.safselvbetjening.generated.hentdokumentoversikt.DokumentInfo
import no.nav.sifinnsynapi.safselvbetjening.generated.hentdokumentoversikt.Journalpost
import org.json.JSONObject
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URL
import java.util.*

@Service
class SøknadService(
    private val repo: SøknadRepository,
    private val oppslagsService: OppslagsService,
    private val dokumentService: DokumentService,
    private val arbeidsgiverMeldingPDFGenerator: ArbeidsgiverMeldingPDFGenerator
) {

    companion object {
        private val mapper = ObjectMapper()
    }

    fun hentSøknader(): List<SøknadDTO> {

        val aktørId = AktørId.valueOf(oppslagsService.hentAktørId()!!.aktør_id)

        val søknadDAOs = repo.findAllByAktørId(aktørId)
        val relevanteBrevKoder: List<String> = søknadDAOs.flatMap { brevkoder[it.søknadstype]!! }
        val dokumentOversikt = dokumentService.hentDokumentOversikt(relevanteBrevKoder)

        return søknadDAOs.map { søknadDAO ->
            val dokumentOversikt1 = dokumentOversikt.filter { it.journalpostId == søknadDAO.journalpostId }
            søknadDAO.tilSøknadDTO(dokumentOversikt1) }
    }

    fun hentSøknad(søknadId: UUID): SøknadDTO {
        val søknadDAO = repo.findById(søknadId).orElseThrow {
            SøknadNotFoundException(søknadId.toString())
        }

        val dokumentOversikt = dokumentService.hentDokumentOversikt(brevkoder[søknadDAO.søknadstype]!!)
            .filter { it.journalpostId == søknadDAO.journalpostId }

        return søknadDAO.tilSøknadDTO(dokumentOversikt)
    }

    fun oppdaterSøknadSaksIdGittJournalpostId(saksId: String, journalpostId: String): SøknadDAO {
        val søknad = repo.findByJournalpostId(journalpostId)
            ?: throw SøknadWithJournalpostIdNotFoundException(journalpostId)

        return repo.save(søknad.copy(saksId = saksId))
    }

    fun SøknadDAO.tilSøknadDTO(dokumentOversikt: List<DokumentDTO>) = SøknadDTO(
        søknadId = id,
        saksId = saksId,
        journalpostId = journalpostId,
        søknadstype = søknadstype,
        status = status,
        opprettet = opprettet,
        endret = endret,
        behandlingsdato = behandlingsdato,
        dokumenter = dokumentOversikt,
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

                arbeidsgiverMeldingPDFGenerator.genererPDF(
                    pleiepengesøknadJson.tilPleiepengerAreidsgivermelding(
                        funnetOrg
                    )
                )
            }

            else -> throw NotSupportedArbeidsgiverMeldingException(søknad.id.toString(), søknad.søknadstype)
        }
    }

    fun søknadGittJournalpostIdEksisterer(journalpostId: String): Boolean {
        return repo.findByJournalpostId(journalpostId) != null
    }
}


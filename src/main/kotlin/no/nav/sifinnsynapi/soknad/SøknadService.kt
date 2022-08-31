package no.nav.sifinnsynapi.soknad

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
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
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
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
        private val logger = LoggerFactory.getLogger(SøknadService::class.java)
    }

    fun hentSøknader(): List<SøknadDTO> {

        val aktørId = AktørId.valueOf(oppslagsService.hentAktørId()!!.aktør_id)

        val søknadDAOs = repo.findAllByAktørId(aktørId)
            .filter { it.søknadstype.gjelderPP() }

        val relevanteBrevKoder: List<String> = søknadDAOs.flatMap { brevkoder[it.søknadstype]!! }
        val dokumentOversikt: List<DokumentDTO> = try {
            dokumentService.hentDokumentOversikt(relevanteBrevKoder)
        } catch (e: Exception) {
            logger.error("Feilet med å hente dokumentoversikt. Returnerer tom liste.", e)
            listOf()
        }

        return søknadDAOs
            .map { søknadDAO ->
            val relevanteDokumenter = dokumentOversikt.filter { it.journalpostId == søknadDAO.journalpostId }
            søknadDAO.tilSøknadDTO(relevanteDokumenter) }
    }

    fun hentSøknad(søknadId: UUID): SøknadDTO {
        val søknadDAO = repo.findById(søknadId).orElseThrow {
            SøknadNotFoundException(søknadId.toString())
        }

        val dokumentOversikt = dokumentService.hentDokumentOversikt(brevkoder[søknadDAO.søknadstype]!!)
            .filter { it.journalpostId == søknadDAO.journalpostId }

        return søknadDAO.tilSøknadDTO(dokumentOversikt)
    }

    fun hentSistInnsendteSøknad(søknadstype: Søknadstype): SøknadDTO? {
        val aktørId = AktørId.valueOf(oppslagsService.hentAktørId()!!.aktør_id)
        return repo.finnSisteSøknadGittAktørIdOgSøknadstype(aktørId = aktørId.aktørId!!, søknadstype = søknadstype.name)
            ?.tilSøknadDTO()
    }

    fun oppdaterSøknadSaksIdGittJournalpostId(saksId: String, journalpostId: String): SøknadDAO {
        val søknad = repo.findByJournalpostId(journalpostId)
            ?: throw SøknadWithJournalpostIdNotFoundException(journalpostId)

        return repo.save(søknad.copy(saksId = saksId))
    }

    fun SøknadDAO.tilSøknadDTO(dokumentOversikt: List<DokumentDTO>? = null): SøknadDTO {
        return SøknadDTO(
            søknadId = id,
            saksId = saksId,
            journalpostId = journalpostId,
            søknadstype = søknadstype,
            status = status,
            opprettet = opprettet,
            endret = endret,
            behandlingsdato = behandlingsdato,
            dokumenter = dokumentOversikt?: listOf(),
            søknad = mapper.readValue(
                søknad,
                object :
                    TypeReference<MutableMap<String, Any>>() {}
            )
        )
    }

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


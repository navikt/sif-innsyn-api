package no.nav.sifinnsynapi.soknad

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.sifinnsynapi.common.AktørId
import no.nav.sifinnsynapi.common.Søknadstype
import no.nav.sifinnsynapi.dokument.DokumentDTO
import no.nav.sifinnsynapi.dokument.DokumentService
import no.nav.sifinnsynapi.dokument.DokumentService.Companion.brevkoder
import no.nav.sifinnsynapi.konsument.pleiepenger.syktbarn.ArbeidsgiverMeldingPDFGenerator
import no.nav.sifinnsynapi.konsument.pleiepenger.syktbarn.PleiepengerJSONObjectUtils.finnOrganisasjon
import no.nav.sifinnsynapi.konsument.pleiepenger.syktbarn.PleiepengerJSONObjectUtils.tilPleiepengerAreidsgivermelding
import no.nav.sifinnsynapi.oppslag.OppslagsService
import no.nav.sifinnsynapi.util.ServletUtils
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.stereotype.Service
import org.springframework.web.ErrorResponseException
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.Charset
import java.util.*
import java.util.stream.Stream

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

    fun finnAlleSøknaderMedUnikeFødselsnummerForSøknadstype(søknadstype: Søknadstype): Stream<SøknadDAO> {
        return repo.finnAlleSøknaderMedUnikeFødselsnummerForSøknadstype(søknadstype.name)
    }
}

class SøknadNotFoundException(søknadId: String) :
    ErrorResponseException(HttpStatus.NOT_FOUND, asProblemDetail(søknadId), null) {
    private companion object {
        private fun asProblemDetail(søknadId: String): ProblemDetail {
            val problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND)
            problemDetail.title = "Søknad ble ikke funnet"
            problemDetail.detail = "Søknad med id $søknadId ble ikke funnet."
            problemDetail.type = URI("/problem-details/søknad-ikke-funnet")
            ServletUtils.currentHttpRequest()?.let {
                problemDetail.instance = URI(URLDecoder.decode(it.requestURL.toString(), Charset.defaultCharset()))
            }
            return problemDetail
        }
    }
}

class SøknadWithJournalpostIdNotFoundException(journalpostId: String) :
    RuntimeException("Søknad med journalpostId = $journalpostId ble ikke funnet.")

class NotSupportedArbeidsgiverMeldingException(søknadId: String, søknadstype: Søknadstype) :
    ErrorResponseException(HttpStatus.BAD_REQUEST, asProblemDetail(søknadId, søknadstype), null) {
    private companion object {
        private fun asProblemDetail(søknadId: String, søknadstype: Søknadstype): ProblemDetail {
            val problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST)
            problemDetail.title = "Arbeidsgivermelding ikke støttet"
            problemDetail.detail =
                "Søknad med søknadId = $søknadId  og søknadstype = $søknadstype støtter ikke arbeidsgivermelding."
            problemDetail.type = URI("/problem-details/arbeidsgivermelding-ikke-støttet")
            ServletUtils.currentHttpRequest()?.let {
                problemDetail.instance = URI(URLDecoder.decode(it.requestURL.toString(), Charset.defaultCharset()))
            }
            return problemDetail
        }
    }
}


package no.nav.sifinnsynapi.dokument

import no.nav.sifinnsynapi.http.DocumentNotFoundException
import org.springframework.stereotype.Service
import java.util.*

@Service
class DokumentService(
        private val repo: DokumentRepository
) {

    fun hentDokument(søknadId: UUID): DokumentDAO? {
        return repo.findBySøknadId(søknadId) ?: throw DocumentNotFoundException(søknadId.toString())

    }
}

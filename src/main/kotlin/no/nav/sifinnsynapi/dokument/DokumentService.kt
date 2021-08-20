package no.nav.sifinnsynapi.dokument

import kotlinx.coroutines.runBlocking
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.sifinnsynapi.safselvbetjening.SafSelvbetjeningService
import no.nav.sifinnsynapi.safselvbetjening.generated.hentdokumentoversikt.Dokumentoversikt
import org.springframework.core.io.ByteArrayResource
import org.springframework.stereotype.Service
import java.util.*

@Service
class DokumentService(
        private val safSelvbetjeningService: SafSelvbetjeningService,
        private val tokenValidationContextHolder: SpringTokenValidationContextHolder
) {

    fun hentDokumentOversikt(): Dokumentoversikt = runBlocking {
        val token = tokenValidationContextHolder.tokenValidationContext.firstValidToken.get()
        val dokumentoversikt = safSelvbetjeningService.hentDokumentoversikt(token.subject)
        dokumentoversikt
    }

    fun hentDokument(journalpostId: String, dokumentInfoId: String, varianFormat: String): ByteArrayResource {
        val dokumentSomBase64 = safSelvbetjeningService.hentDokument(journalpostId, dokumentInfoId, varianFormat)
        return ByteArrayResource(Base64.getDecoder().decode(dokumentSomBase64))
    }
}

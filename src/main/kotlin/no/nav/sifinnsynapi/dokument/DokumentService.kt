package no.nav.sifinnsynapi.dokument

import kotlinx.coroutines.runBlocking
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.sifinnsynapi.safselvbetjening.ArkivertDokument
import no.nav.sifinnsynapi.safselvbetjening.SafSelvbetjeningService
import no.nav.sifinnsynapi.safselvbetjening.generated.hentdokumentoversikt.Dokumentoversikt
import org.slf4j.LoggerFactory
import org.springframework.core.io.ByteArrayResource
import org.springframework.stereotype.Service
import java.util.*

@Service
class DokumentService(
    private val safSelvbetjeningService: SafSelvbetjeningService,
    private val tokenValidationContextHolder: SpringTokenValidationContextHolder
) {
    private companion object {
        private val logger = LoggerFactory.getLogger(DokumentService::class.java)
    }

    fun hentDokumentOversikt(): Dokumentoversikt = runBlocking {
        val token = tokenValidationContextHolder.tokenValidationContext.firstValidToken.get()
        safSelvbetjeningService.hentDokumentoversikt(token.subject)
    }

    fun hentDokument(journalpostId: String, dokumentInfoId: String, varianFormat: String): ArkivertDokument {
        return safSelvbetjeningService.hentDokument(journalpostId, dokumentInfoId, varianFormat)
    }
}

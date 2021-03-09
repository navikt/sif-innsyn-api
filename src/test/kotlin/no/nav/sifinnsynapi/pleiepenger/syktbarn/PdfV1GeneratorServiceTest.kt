package no.nav.sifinnsynapi.pleiepenger.syktbarn

import org.junit.jupiter.api.Test
import java.io.File

class PdfV1GeneratorServiceTest {

    private companion object {
        private val generator = PdfV1GeneratorService()
    }

    @Test
    fun name() {
        var id = "1-full-søknad"
        var pdf = generator.generateSoknadOppsummeringPdf(
            melding = ""
        )
        File(pdfPath(soknadId = id)).writeBytes(pdf)
    }

    private fun pdfPath(soknadId: String) = "${System.getProperty("user.dir")}/generated-pdf-$soknadId.pdf"
}

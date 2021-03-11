package no.nav.sifinnsynapi.pleiepenger.syktbarn

import no.nav.sifinnsynapi.soknad.PleiepengerArbeidsgiverMelding
import no.nav.sifinnsynapi.soknad.SøknadsPeriode
import org.junit.jupiter.api.Test
import java.io.File
import java.time.LocalDate

class ArbeidsgiverMeldingPDFGeneratorTest {

    private companion object {
        private val generator = ArbeidsgiverMeldingPDFGenerator()
    }

    @Test
    fun name() {
        var id = "1-full-søknad"
        var pdf = generator.genererPDF(
            melding = PleiepengerArbeidsgiverMelding(
                arbeidstakernavn = "Navn Navnesen",
                arbeidsgivernavn = "Snill Torpedo",
                søknadsperiode = SøknadsPeriode(
                    fraOgMed = LocalDate.now().minusDays(10),
                    tilOgMed = LocalDate.now()
                )
            )
        )
        File(pdfPath(soknadId = id)).writeBytes(pdf)
    }

    private fun pdfPath(soknadId: String) = "${System.getProperty("user.dir")}/generated-pdf-$soknadId.pdf"
}

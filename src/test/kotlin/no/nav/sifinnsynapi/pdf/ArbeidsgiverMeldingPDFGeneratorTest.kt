package no.nav.sifinnsynapi.pdf

import no.nav.sifinnsynapi.konsument.pleiepenger.syktbarn.ArbeidsgiverMeldingPDFGenerator
import no.nav.sifinnsynapi.konsument.pleiepenger.syktbarn.PleiepengerArbeidsgiverMelding
import no.nav.sifinnsynapi.konsument.pleiepenger.syktbarn.SøknadsPeriode
import org.junit.jupiter.api.Test
import java.io.File
import java.time.LocalDate

class ArbeidsgiverMeldingPDFGeneratorTest {

    @Test
    fun pdf() {
        val pdf = ArbeidsgiverMeldingPDFGenerator().genererPDF(
            melding = PleiepengerArbeidsgiverMelding(
                arbeidstakernavn = "Ola Nordmann",
                arbeidsgivernavn = "Sjokkerende Elektriker",
                søknadsperiode = SøknadsPeriode(
                    fraOgMed = LocalDate.now().minusWeeks(1),
                    tilOgMed = LocalDate.now().plusWeeks(1)
                )
            )
        )
        File(pdfPath("Bekreftelse til arbeidsgiver")).writeBytes(pdf)
    }

    private fun pdfPath(filnavn: String) = "${System.getProperty("user.dir")}/generated-pdf-$filnavn.pdf"
}

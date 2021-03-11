package no.nav.sifinnsynapi.pleiepenger.syktbarn

import no.nav.sifinnsynapi.pdf.PDFGenerator
import no.nav.sifinnsynapi.soknad.PleiepengerArbeidsgiverMelding
import org.springframework.stereotype.Service
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime

@Service
class ArbeidsgiverMeldingPDFGenerator : PDFGenerator<PleiepengerArbeidsgiverMelding>() {

    override val templateNavn: String
        get() = "informasjonsbrev-til-arbeidsgiver"


    override fun PleiepengerArbeidsgiverMelding.tilMap(): Map<String, Any?> = mapOf(
        "arbeidsgiver_navn" to arbeidsgivernavn?.capitalize(),
        "arbeidstaker_navn" to arbeidstakernavn.capitalize(),
        "periode" to mapOf(
            "fom" to DATE_FORMATTER.format(søknadsperiode.fraOgMed),
            "tom" to DATE_FORMATTER.format(søknadsperiode.tilOgMed)
        ),
        "tidspunkt" to DATE_TIME_FORMATTER.format(ZonedDateTime.now(UTC))
    )

    override val bilder: Map<String, String>
        get() = mapOf(
            "Checkbox_off.png" to loadPng("Checkbox_off"),
            "Checkbox_on.png" to loadPng("Checkbox_on"),
            "Hjelp.png" to loadPng("Hjelp"),
            "Navlogo.png" to loadPng("Navlogo"),
            "Fritekst.png" to loadPng("Fritekst")
        )
}


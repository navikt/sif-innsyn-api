package no.nav.sifinnsynapi.pleiepenger.syktbarn

import com.github.jknack.handlebars.Context
import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.context.MapValueResolver
import com.github.jknack.handlebars.io.ClassPathTemplateLoader
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import no.nav.sifinnsynapi.soknad.PleiepengerArbeidsgiverMelding
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.ZoneId
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class PdfV1GeneratorService {
    private companion object {
        private const val ROOT = "handlebars"
        private const val SOKNAD = "informasjonsbrev-til-arbeidsgiver"

        private val REGULAR_FONT = ClassPathResource("$ROOT/fonts/SourceSansPro-Regular.ttf").inputStream.readAllBytes()
        private val BOLD_FONT = ClassPathResource("$ROOT/fonts/SourceSansPro-Bold.ttf").inputStream.readAllBytes()
        private val ITALIC_FONT = ClassPathResource("$ROOT/fonts/SourceSansPro-Italic.ttf").inputStream.readAllBytes()


        private val images = loadImages()
        private val handlebars = Handlebars(ClassPathTemplateLoader("/$ROOT")).apply {
            registerHelper("image", Helper<String> { context, _ ->
                if (context == null) "" else images[context]
            })
            registerHelper("eq", Helper<String> { context, options ->
                if (context == options.param(0)) options.fn() else options.inverse()
            })
            registerHelper("eqTall", Helper<Int> { context, options ->
                if (context == options.param(0)) options.fn() else options.inverse()
            })
            registerHelper("fritekst", Helper<String> { context, _ ->
                if (context == null) "" else {
                    val text = Handlebars.Utils.escapeExpression(context)
                        .toString()
                        .replace(Regex("\\r\\n|[\\n\\r]"), "<br/>")
                    Handlebars.SafeString(text)
                }
            })
            registerHelper("jaNeiSvar", Helper<Boolean> { context, _ ->
                if (context == true) "Ja" else "Nei"
            })

            infiniteLoops(true)
        }

        private val soknadTemplate = handlebars.compile(SOKNAD)

        private val ZONE_ID = ZoneId.of("Europe/Oslo")
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZONE_ID)
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZONE_ID)

        private fun loadPng(name: String): String {
            val bytes = ClassPathResource("$ROOT/images/$name.png").inputStream.readAllBytes()
            val base64string = Base64.getEncoder().encodeToString(bytes)
            return "data:image/png;base64,$base64string"
        }

        private fun loadImages() = mapOf(
            "Checkbox_off.png" to loadPng("Checkbox_off"),
            "Checkbox_on.png" to loadPng("Checkbox_on"),
            "Hjelp.png" to loadPng("Hjelp"),
            "Navlogo.png" to loadPng("Navlogo"),
            "Fritekst.png" to loadPng("Fritekst"),
            "InformasjonIkon.png" to loadPng("InformationFilled-24px")
        )
    }

    internal fun generateSoknadOppsummeringPdf(
        melding: PleiepengerArbeidsgiverMelding
    ): ByteArray {
        soknadTemplate.apply(
            Context
                .newBuilder(
                    mapOf(
                        "arbeidsgiver_navn" to melding.arbeidsgivernavn?.capitalize(),
                        "arbeidstaker_navn" to melding.arbeidstakernavn.capitalize(),
                        "periode" to mapOf(
                            "fom" to DATE_FORMATTER.format(melding.søknadsperiode.fraOgMed),
                            "tom" to DATE_FORMATTER.format(melding.søknadsperiode.tilOgMed)
                        ),
                        "tidspunkt" to DATE_TIME_FORMATTER.format(ZonedDateTime.now(UTC))
                    )
                )
                .resolver(MapValueResolver.INSTANCE)
                .build()
        ).let { html ->
            val outputStream = ByteArrayOutputStream()

            PdfRendererBuilder()
                .useFastMode()
                .usePdfUaAccessbility(true)
                .withHtmlContent(html, "")
                .medFonter()
                .toStream(outputStream)
                .buildPdfRenderer()
                .createPDF()

            return outputStream.use {
                it.toByteArray()
            }
        }
    }

    private fun PdfRendererBuilder.medFonter() =
        useFont(
            { ByteArrayInputStream(REGULAR_FONT) },
            "Source Sans Pro",
            400,
            BaseRendererBuilder.FontStyle.NORMAL,
            false
        )
            .useFont(
                { ByteArrayInputStream(BOLD_FONT) },
                "Source Sans Pro",
                700,
                BaseRendererBuilder.FontStyle.NORMAL,
                false
            )
            .useFont(
                { ByteArrayInputStream(ITALIC_FONT) },
                "Source Sans Pro",
                400,
                BaseRendererBuilder.FontStyle.ITALIC,
                false
            )
}

fun String.capitalizeName(): String = split(" ").joinToString(" ") { it.toLowerCase().capitalize() }

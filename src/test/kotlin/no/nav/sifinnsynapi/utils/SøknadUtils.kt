package no.nav.sifinnsynapi.utils

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.sifinnsynapi.common.JournalfortMelding
import no.nav.sifinnsynapi.common.Metadata
import no.nav.sifinnsynapi.common.SøknadsHendelse
import no.nav.sifinnsynapi.common.TopicEntry
import no.nav.sifinnsynapi.soknad.SøknadDTO
import java.time.ZonedDateTime
import java.util.*


fun List<SøknadDTO>.somJson(mapper: ObjectMapper) = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)
fun TopicEntry.somJson(mapper: ObjectMapper) = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)

val defaultMetadata = Metadata(
        version = 1,
        correlationId = UUID.randomUUID().toString(),
        requestId = UUID.randomUUID().toString()
)

val defaultHendelse = TopicEntry(
        data = SøknadsHendelse(
                metadata = defaultMetadata,
                melding = mapOf(
                        "soknadId" to UUID.randomUUID().toString(),
                        "mottatt" to ZonedDateTime.now(),
                        "søker" to mapOf(
                                "fødselsnummer" to "1234567",
                                "aktørId" to "123456"
                        )
                ),
                journalførtMelding = JournalfortMelding(
                        journalpostId = "123456789"
                ),
                pdfDokument = file2ByteArray("eksempel-søknad.pdf")
        )
)





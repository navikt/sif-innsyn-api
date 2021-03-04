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
fun SøknadDTO.somJson(mapper: ObjectMapper) = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)
fun TopicEntry.somJson(mapper: ObjectMapper) = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)

val defaultMetadata = Metadata(
        version = 1,
        correlationId = UUID.randomUUID().toString(),
        requestId = UUID.randomUUID().toString()
)

fun defaultHendelse(søknadIdKey: String = "søknadId", søknadId: UUID = UUID.randomUUID(), journalpostId: String = "123456789") = TopicEntry(
        data = SøknadsHendelse(
                metadata = defaultMetadata,
                melding = mapOf(
                        "$søknadIdKey" to "$søknadId",
                        "mottatt" to ZonedDateTime.now(),
                        "søker" to mapOf(
                                "fødselsnummer" to "1234567",
                                "aktørId" to "123456"
                        )
                ),
                journalførtMelding = JournalfortMelding(
                        journalpostId = "$journalpostId"
                )
        )
)

fun defaultHendelseOmsorgsdagerMelding(
        søknadIdKey: String = "søknadId",
        søknadId: UUID = UUID.randomUUID(),
        journalpostId: String = "123456789",
        type: String
) = TopicEntry(
        data = SøknadsHendelse(
                metadata = defaultMetadata,
                melding = mapOf(
                        "$søknadIdKey" to "$søknadId",
                        "mottatt" to ZonedDateTime.now(),
                        "søker" to mapOf(
                                "fødselsnummer" to "1234567",
                                "aktørId" to "123456"
                        ),
                        "type" to "$type"
                ),
                journalførtMelding = JournalfortMelding(
                        journalpostId = "$journalpostId"
                )
        )
)


fun defaultHendelseK9Ettersending(
        søknadIdKey: String = "soknadId",
        søknadId: UUID = UUID.randomUUID(),
        journalpostId: String = "123456789",
        søknadstype: String
) = TopicEntry(
        data = SøknadsHendelse(
                metadata = defaultMetadata,
                melding = mapOf(
                        "$søknadIdKey" to "$søknadId",
                        "mottatt" to ZonedDateTime.now(),
                        "søker" to mapOf(
                                "fødselsnummer" to "1234567",
                                "aktørId" to "123456"
                        ),
                        "søknadstype" to "$søknadstype"
                ),
                journalførtMelding = JournalfortMelding(
                        journalpostId = "$journalpostId"
                )
        )
)
package no.nav.sifinnsynapi.utils

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import no.nav.sifinnsynapi.common.JournalfortMelding
import no.nav.sifinnsynapi.common.Metadata
import no.nav.sifinnsynapi.common.SøknadsHendelse
import no.nav.sifinnsynapi.common.TopicEntry
import no.nav.sifinnsynapi.konsument.ettersending.K9EttersendingKonsument
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

fun defaultHendelse(
    søknadIdKey: String = "søknadId",
    søknadId: UUID = UUID.randomUUID(),
    journalpostId: String = "123456789"
) = TopicEntry(
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

fun defaultHendelsePPEndringsmelding(
    søknadIdKey: String = "søknadId",
    søknadId: UUID = UUID.randomUUID(),
    journalpostId: String = "123456789"
) = TopicEntry(
    data = SøknadsHendelse(
        metadata = defaultMetadata,
        melding = mapOf(
            "søker" to mapOf(
                "fødselsnummer" to "1234567",
                "aktørId" to "123456"
            ),
            "k9FormatSøknad" to mapOf(
                "$søknadIdKey" to "$søknadId",
                "mottattDato" to ZonedDateTime.parse("2022-01-31T12:20:47.151403+01:00")
            ),
            "dokumentId" to listOf("123", "456")
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
    ettersendelsestype: K9EttersendingKonsument.Companion.Ettersendelsestype
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
            "søknadstype" to "${ettersendelsestype.name}"
        ),
        journalførtMelding = JournalfortMelding(
            journalpostId = "$journalpostId"
        )
    )
)


fun defaultJournalfoeringHendelseRecord(journalpostId: Long) = JournalfoeringHendelseRecord(
    UUID.randomUUID().toString(), 1, "EndeligJournalført", journalpostId,
    "J", "OMS", "OMS", "NAV_NO", "", ""
)

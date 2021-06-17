package no.nav.sifinnsynapi.konsument.dokumentjournalforing

import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service

@Service
class DokumentJournalføringHendelseKonsument {

    companion object {
        private val logger = LoggerFactory.getLogger(DokumentJournalføringHendelseKonsument::class.java)
    }

    @KafkaListener(
        topics = ["aapen-dok-journalfoering-v1-q1"],
        id = "dokument-journalføring-listener",
        groupId = "#{'\${kafka.onprem.consumer.group-id}'}",
        containerFactory = "defaultKafkaJsonListenerContainerFactor",
        autoStartup = "true"
    )
    fun konsumer(
        @Payload melding: String
    ) {
        val melding = JSONObject(melding)
        logger.info("Mottok hendelse om dokumentjournalføring: {}", melding.toString(2))
    }
}

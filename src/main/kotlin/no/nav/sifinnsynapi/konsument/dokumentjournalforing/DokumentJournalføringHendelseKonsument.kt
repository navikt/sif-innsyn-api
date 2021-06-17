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
        topics = ["#{'\${topic.listener.dok-journalfoering-v1.navn}'}"],
        id = "#{'\${topic.listener.dok-journalfoering-v1.id}'}",
        groupId = "#{'\${kafka.onprem.consumer.group-id}'}",
        containerFactory = "defaultKafkaJsonListenerContainerFactor",
        autoStartup = "#{'\${topic.listener.dok-journalfoering-v1.bryter}'}"
    )
    fun konsumer(
        @Payload melding: String
    ) {
        val melding = JSONObject(melding)
        logger.info("Mottok hendelse om dokumentjournalføring: {}", melding.toString(2))
    }
}

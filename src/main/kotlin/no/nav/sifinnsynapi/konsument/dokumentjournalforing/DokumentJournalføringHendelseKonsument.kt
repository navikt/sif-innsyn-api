package no.nav.sifinnsynapi.konsument.dokumentjournalforing

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service

@Service
class DokumentJournalføringHendelseKonsument(
    private val objectMapper: ObjectMapper
) {

    companion object {
        private val logger = LoggerFactory.getLogger(DokumentJournalføringHendelseKonsument::class.java)
    }

    @KafkaListener(
        topics = ["#{'\${topic.listener.dok-journalfoering-v1.navn}'}"],
        id = "#{'\${topic.listener.dok-journalfoering-v1.id}'}",
        groupId = "#{'\${kafka.onprem.consumer.group-id}'}",
        containerFactory = "dokJournalføringKafkaJsonListenerContainerFactor",
        autoStartup = "#{'\${topic.listener.dok-journalfoering-v1.bryter}'}"
    )
    fun konsumer(
        @Payload cr: ConsumerRecord<String, Any>
    ) {
        logger.info("Mottok hendelse om dokumentjournalføring (CR): {}", cr)
        val jsonMelding = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(cr)
        logger.info("Mottok hendelse om dokumentjournalføring(CR value): {}", jsonMelding)
    }
}

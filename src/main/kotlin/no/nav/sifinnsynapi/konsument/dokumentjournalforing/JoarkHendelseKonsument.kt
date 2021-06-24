package no.nav.sifinnsynapi.konsument.dokumentjournalforing

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service

@Service
class JoarkHendelseKonsument {

    companion object {
        private val logger = LoggerFactory.getLogger(JoarkHendelseKonsument::class.java)
    }

    @KafkaListener(
        topics = ["#{'\${topic.listener.dok-journalfoering-v1.navn}'}"],
        id = "#{'\${topic.listener.dok-journalfoering-v1.id}'}",
        groupId = "#{'\${kafka.onprem.consumer.group-id}'}",
        containerFactory = "joarkKafkaJsonListenerContainerFactor",
        autoStartup = "#{'\${topic.listener.dok-journalfoering-v1.bryter}'}",
        properties = ["auto.offset.reset=latest"]
    )
    fun konsumer(
        @Payload cr: ConsumerRecord<Long, JournalfoeringHendelseRecord>
    ) {
        logger.info("Mottatt journalføringshendelse med status: {}", cr.value().hendelsesType)
    }
}

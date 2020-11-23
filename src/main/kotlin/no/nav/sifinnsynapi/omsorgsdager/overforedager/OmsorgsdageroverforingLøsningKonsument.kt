package no.nav.sifinnsynapi.omsorgsdager.overforedager

import no.nav.k9.rapid.losning.OverføreOmsorgsdagerLøsning
import no.nav.sifinnsynapi.soknad.SøknadRepository
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OmsorgsdagerOverforingLøsningKonsument(
        private val repository: SøknadRepository,
        @Value("\${topic.listener.omd-overforing-losning.dry-run}") private val dryRun: Boolean
) {
    companion object {
        private val logger = LoggerFactory.getLogger(OmsorgsdagerOverforingLøsningKonsument::class.java)
        private val YTELSE = "'løsning på omsorgsdager - overføring'"
    }

    @Transactional
    @KafkaListener(
            topics = ["#{'\${topic.listener.omd-overforing-losning.navn}'}"],
            id = "#{'\${topic.listener.omd-overforing-losning.id}'}",
            groupId = "#{'\${spring.kafka.consumer.group-id}'}",
            containerFactory = "kafkaK9RapidJsonListenerContainerFactory",
            autoStartup = "#{'\${topic.listener.omd-overforing-losning.bryter}'}"
    )
    fun konsumer(
            @Payload hendelse: ConsumerRecord<String, OverføreOmsorgsdagerLøsning>
    ) {

        logger.info("Mottatt $YTELSE. {}", hendelse)
    }
}


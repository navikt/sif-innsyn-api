package no.nav.sifinnsynapi.omsorgsdager.overforedager

import no.nav.k9.rapid.losning.OverføreOmsorgsdagerLøsning
import no.nav.sifinnsynapi.soknad.SøknadRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OmsorgsdagerOverforingHendelseKonsument(
        private val repository: SøknadRepository,
        @Value("\${topic.listener.omd-overforing.dry-run}") private val dryRun: Boolean
) {
    companion object {
        private val logger = LoggerFactory.getLogger(OmsorgsdagerOverforingHendelseKonsument::class.java)
        private val YTELSE = "'omsorgsdager - overføring'"
    }

    @Transactional
    @KafkaListener(
            topics = ["#{'\${topic.listener.omd-overforing.navn}'}"],
            id = "#{'\${topic.listener.omd-overforing.id}'}",
            groupId = "#{'\${spring.kafka.consumer.group-id}'}",
            containerFactory = "kafkaK9RapidJsonListenerContainerFactory",
            autoStartup = "#{'\${topic.listener.omd-overforing.bryter}'}"
    )
    fun konsumer(
            @Payload hendelse: OverføreOmsorgsdagerLøsning
    ) {
        val (id, løsning) = hendelse
        logger.info("Mottatt løsning: {}", løsning)
    }
}


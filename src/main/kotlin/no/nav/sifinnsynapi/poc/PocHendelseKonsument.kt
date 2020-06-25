package no.nav.sifinnsynapi.poc

import no.nav.sifinnsynapi.config.Topics.INNSYN_MOTTATT
import no.nav.sifinnsynapi.util.Constants.NAV_CALL_ID
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PocHendelseKonsument(
        private val søknadRepository: SøknadRepository
) {

    @Transactional
    @KafkaListener(topics = [INNSYN_MOTTATT], groupId = "#{'\${spring.kafka.consumer.group-id}'}", containerFactory = "kafkaJsonListenerContainerFactory")
    fun konsumer(@Payload hendelse: SøknadsHendelse,
                 @Header(name = NAV_CALL_ID, required = false) callId: String?) {
        LOG.info("Mottok hendelse {}", hendelse)

        val mapTilSøknadDAO = hendelse.tilSøknadDAO()
        søknadRepository.save(mapTilSøknadDAO)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(PocHendelseKonsument::class.java)
    }
}

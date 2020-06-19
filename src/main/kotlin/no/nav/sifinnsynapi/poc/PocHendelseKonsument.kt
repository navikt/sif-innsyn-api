package no.nav.sifinnsynapi.poc

import no.nav.sifinnsynapi.util.Constants.NAV_CALL_ID
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PocHendelseKonsument {

    @Transactional
    @KafkaListener(topics = ["privat-sif-innsyn-mottak"], groupId = "#{'\${spring.kafka.consumer.group-id}'}")
    fun konsumer(@Payload hendelse: String,
                 @Header(name = NAV_CALL_ID, required = false) callId: String?) {
        LOG.info("Mottok inntektsmeldinghendelse {}", hendelse)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(PocHendelseKonsument::class.java)
    }
}

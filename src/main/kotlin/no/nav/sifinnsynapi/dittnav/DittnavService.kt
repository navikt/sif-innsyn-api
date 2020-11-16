package no.nav.sifinnsynapi.dittnav

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.sifinnsynapi.config.Topics.K9_DITTNAV_VARSEL_BESKJED
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class DittnavService(
        private val kafkaTemplate: KafkaTemplate<String, String>,
        private val objectMapper: ObjectMapper
) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(DittnavService::class.java)
    }

    fun sendBeskjed(søknadId: String, k9Beskjed: K9Beskjed) {
        log.info("Sender ut dittnav beskjed med eventID: {}", søknadId)
        kafkaTemplate.send(ProducerRecord(
                K9_DITTNAV_VARSEL_BESKJED,
                søknadId,
                k9Beskjed.somJson(objectMapper)
        ))
    }
}

fun K9Beskjed.somJson(mapper: ObjectMapper) = mapper.writeValueAsString(this)



package no.nav.sifinnsynapi.dittnav

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.sifinnsynapi.config.Topics.INNSYN_MOTTATT
import no.nav.sifinnsynapi.pleiepenger.syktbarn.InnsynMelding
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

    fun sendBeskjed(søknadId: String, innsynMelding: InnsynMelding) {
        log.info("Sender ut dittnav beskjed med eventID: {}", søknadId)
        kafkaTemplate.send(ProducerRecord(
                INNSYN_MOTTATT,
                søknadId,
                innsynMelding.somJson(objectMapper)
        ))
    }
}

fun InnsynMelding.somJson(mapper: ObjectMapper) = mapper.writeValueAsString(this)



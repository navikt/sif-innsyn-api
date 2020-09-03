package no.nav.sifinnsynapi.dittnav

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.sifinnsynapi.config.Topics.INNSYN_MOTTATT
import no.nav.sifinnsynapi.pleiepenger.syktbarn.InnsynMelding
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class DittnavService(
        private val kafkaTemplate: KafkaTemplate<String, String>,
        private val objectMapper: ObjectMapper
) {

    fun sendBeskjed(søknadId: String, innsynMelding: InnsynMelding) {
        kafkaTemplate.send(ProducerRecord(
                INNSYN_MOTTATT,
                søknadId,
                innsynMelding.somJson(objectMapper)
        ))
    }
}

fun InnsynMelding.somJson(mapper: ObjectMapper) = mapper.writeValueAsString(this)



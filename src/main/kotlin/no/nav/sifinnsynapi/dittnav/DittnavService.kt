package no.nav.sifinnsynapi.dittnav

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.sifinnsynapi.config.Topics.K9_DITTNAV_VARSEL_BESKJED
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DittnavService(
        private val kafkaTemplate: KafkaTemplate<String, String>,
        private val objectMapper: ObjectMapper
) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(DittnavService::class.java)
    }

    @Transactional
    fun sendBeskjed(søknadId: String, k9Beskjed: K9Beskjed) {
        log.info("Sender ut dittnav beskjed med eventID: {}", søknadId)
        return kafkaTemplate.executeInTransaction {
            it.send(ProducerRecord(
                    K9_DITTNAV_VARSEL_BESKJED,
                    søknadId,
                    k9Beskjed.somJson(objectMapper)
            ))
                    .addCallback(
                            { result ->
                                result?.let {
                                    log.info("Sendte melding {} med offset {} på {}", result.producerRecord, result.recordMetadata.offset(), result.producerRecord.topic());
                                }
                            },
                            { ex ->
                                log.warn("Kunne ikke sende melding {} på {}", k9Beskjed, K9_DITTNAV_VARSEL_BESKJED, ex);
                                throw ex
                            }
                    )
        }
    }
}

fun K9Beskjed.somJson(mapper: ObjectMapper) = mapper.writeValueAsString(this)



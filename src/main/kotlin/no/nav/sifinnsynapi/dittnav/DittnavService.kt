package no.nav.sifinnsynapi.dittnav

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.sifinnsynapi.config.Topics.K9_DITTNAV_VARSEL_BESKJED_AIVEN
import no.nav.sifinnsynapi.config.Topics.K9_DITTNAV_VARSEL_MICROFRONTEND
import no.nav.sifinnsynapi.util.MDCUtil
import org.apache.kafka.clients.producer.ProducerRecord
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service

@Service
class DittnavService(
    private val aivenKafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(DittnavService::class.java)
    }

    fun sendBeskjedAiven(k9Beskjed: K9Beskjed) {
        log.info(
            "Sender ut dittnav beskjed til aiven med eventID: {} for søknadId: {}",
            k9Beskjed.eventId,
            k9Beskjed.grupperingsId
        )
        aivenKafkaTemplate.send(
            ProducerRecord(
                K9_DITTNAV_VARSEL_BESKJED_AIVEN,
                k9Beskjed.grupperingsId,
                k9Beskjed.somJson(objectMapper)
            )
        )
            .exceptionally { ex: Throwable ->
                log.warn("Kunne ikke sende melding {} på {}", k9Beskjed, K9_DITTNAV_VARSEL_BESKJED_AIVEN, ex)
                throw ex
            }
            .thenAccept { result: SendResult<String, String> ->
                log.info(
                    "Sendte melding med offset {} på {}",
                    result.recordMetadata.offset(),
                    result.producerRecord.topic()
                )
            }
    }

    fun toggleMicrofrontend(k9Microfrontend: K9Microfrontend) {
        log.info("Sender ut dittnav microfrontend event til aiven med")
        aivenKafkaTemplate.send(
            ProducerRecord(
                K9_DITTNAV_VARSEL_MICROFRONTEND,
                k9Microfrontend.metadata.correlationId,
                k9Microfrontend.somJson(objectMapper)
            )
        )
            .exceptionally { ex: Throwable ->
                log.warn("Kunne ikke sende microfrontend event til {}", K9_DITTNAV_VARSEL_MICROFRONTEND, ex)
                throw ex
            }.thenAccept {
                val anonymisertUtkast = JSONObject(it.producerRecord.value())
                anonymisertUtkast.remove("ident") // Fjerner ident fra microfrontend event før det logges.
                log.info("Microfrontend event sendt til ${K9_DITTNAV_VARSEL_MICROFRONTEND}. {}", anonymisertUtkast)
            }
    }
}

fun K9Beskjed.somJson(mapper: ObjectMapper) = mapper.writeValueAsString(this)
fun K9Microfrontend.somJson(mapper: ObjectMapper) = mapper.writeValueAsString(this)

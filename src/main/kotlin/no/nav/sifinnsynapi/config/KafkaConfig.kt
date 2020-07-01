package no.nav.sifinnsynapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.JsonObject
import no.nav.sifinnsynapi.common.AktørId
import no.nav.sifinnsynapi.common.TopicEntry
import no.nav.sifinnsynapi.omsorgspenger.OmsorgspengerutbetalingSNFHendelseKonsument
import no.nav.sifinnsynapi.poc.SøknadRepository
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.converter.JsonMessageConverter

@Configuration
class KafkaConfig(
        @Suppress("SpringJavaInjectionPointsAutowiringInspection") val kafkaTemplate: KafkaTemplate<String, Any>,
        val objectMapper: ObjectMapper,
        val søknadRepository: SøknadRepository) {
    companion object{
        private val log = LoggerFactory.getLogger(KafkaConfig::class.java)
    }

    @Bean
    fun kafkaJsonListenerContainerFactory(consumerFactory: ConsumerFactory<String, String>): KafkaListenerContainerFactory<*> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.consumerFactory = consumerFactory
        factory.setReplyTemplate(kafkaTemplate)
        factory.setMessageConverter(JsonMessageConverter(objectMapper))
        factory.setRecordFilterStrategy {
            val topicEntry = objectMapper.readValue(it.value(), TopicEntry::class.java).data
            val søker = JSONObject(topicEntry.melding).getJSONObject("søker")
            when(søknadRepository.existsSøknadDAOByAktørIdAndJournalpostId(AktørId(søker.getString("aktørId")), topicEntry.journalførtMelding.journalpostId)){
                true -> {
                    log.info("Fant duplikat, skipper deserialisering")
                    true
                }
                false -> {
                    log.info("Fant IKKE duplikat, deserialiserer")
                    false
                }
            }
        }
        return factory
    }
}
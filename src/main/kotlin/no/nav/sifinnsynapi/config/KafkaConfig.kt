package no.nav.sifinnsynapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.converter.JsonMessageConverter

@Configuration
class KafkaConfig(val kafkaTemplate: KafkaTemplate<String, Any>, val objectMapper: ObjectMapper) {

    @Bean
    fun kafkaJsonListenerContainerFactory(consumerFactory: ConsumerFactory<String, String>): KafkaListenerContainerFactory<*> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.consumerFactory = consumerFactory
        factory.setReplyTemplate(kafkaTemplate)
        factory.setMessageConverter(JsonMessageConverter(objectMapper))
        return factory
    }
}
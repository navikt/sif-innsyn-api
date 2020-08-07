package no.nav.sifinnsynapi.utils

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.sifinnsynapi.common.TopicEntry
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.utils.KafkaTestUtils
import java.util.*

fun EmbeddedKafkaBroker.creatKafkaProducer(): Producer<String, Any> {
    return DefaultKafkaProducerFactory<String, Any>(HashMap(KafkaTestUtils.producerProps(this))).createProducer()
}

fun Producer<String, Any>.leggPåTopic(hendelse: TopicEntry, topic: String, mapper: ObjectMapper) {
    this.send(ProducerRecord(topic, hendelse.somJson(mapper)))
    this.flush()
}
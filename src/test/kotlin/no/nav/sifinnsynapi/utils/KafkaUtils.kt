package no.nav.sifinnsynapi.utils

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.sifinnsynapi.common.TopicEntry
import no.nav.sifinnsynapi.config.Topics.K9_DITTNAV_VARSEL_BESKJED
import no.nav.sifinnsynapi.dittnav.K9Beskjed
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.utils.KafkaTestUtils
import java.time.Duration
import java.util.*

fun EmbeddedKafkaBroker.opprettKafkaProducer(): Producer<String, Any> {
    return DefaultKafkaProducerFactory<String, Any>(HashMap(KafkaTestUtils.producerProps(this))).createProducer()
}

fun Producer<String, Any>.leggPåTopic(hendelse: TopicEntry, topic: String, mapper: ObjectMapper) {
    this.send(ProducerRecord(topic, hendelse.somJson(mapper)))
    this.flush()
}

fun EmbeddedKafkaBroker.opprettDittnavConsumer(topic: String = K9_DITTNAV_VARSEL_BESKJED): Consumer<String, K9Beskjed> {
    val consumerProps = KafkaTestUtils.consumerProps("dittnv-consumer", "true", this)
    consumerProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = "org.apache.kafka.common.serialization.StringDeserializer"
    consumerProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = "org.apache.kafka.common.serialization.StringDeserializer"

    val consumer = DefaultKafkaConsumerFactory<String, K9Beskjed>(HashMap(consumerProps)).createConsumer()
    consumer.subscribe(listOf(topic))
    return consumer
}

fun Consumer<String, K9Beskjed>.lesMelding(søknadId: String, topic: String = K9_DITTNAV_VARSEL_BESKJED): List<ConsumerRecord<String, K9Beskjed>> {
    seekToBeginning(assignment())
    val consumerRecords = this.poll(Duration.ofSeconds(1))
    return consumerRecords
            .records(topic)
            .filter { it.key() == søknadId }
}

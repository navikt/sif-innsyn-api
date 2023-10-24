package no.nav.sifinnsynapi.utils

import com.fasterxml.jackson.databind.ObjectMapper
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import no.nav.sifinnsynapi.common.TopicEntry
import no.nav.sifinnsynapi.config.Topics.AAPEN_DOK_JOURNALFØRING_V1
import no.nav.sifinnsynapi.config.Topics.K9_DITTNAV_VARSEL_BESKJED_AIVEN
import no.nav.sifinnsynapi.dittnav.K9Beskjed
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.StringDeserializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.utils.KafkaTestUtils
import java.time.Duration
import java.util.*

fun EmbeddedKafkaBroker.opprettKafkaProducer(): Producer<String, Any> {
    val producerProps = KafkaTestUtils.producerProps(this)
    producerProps[ProducerConfig.CLIENT_ID_CONFIG] = "sif-innsyn-api-producer-${UUID.randomUUID()}"
    producerProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = "org.apache.kafka.common.serialization.StringSerializer"
    producerProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] =
        "org.apache.kafka.common.serialization.StringSerializer"
    return DefaultKafkaProducerFactory<String, Any>(producerProps)
        .createProducer()
}

fun Producer<String, Any>.leggPåTopic(hendelse: TopicEntry, topic: String, mapper: ObjectMapper): RecordMetadata {
    return send(ProducerRecord(topic, hendelse.somJson(mapper))).get()
}

fun EmbeddedKafkaBroker.opprettJoarkKafkaProducer(): Producer<Long, JournalfoeringHendelseRecord> {
    val producerProps = KafkaTestUtils.producerProps(this)
    producerProps[ProducerConfig.CLIENT_ID_CONFIG] = "joark-producer-${UUID.randomUUID()}"
    producerProps[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = "org.apache.kafka.common.serialization.StringSerializer"
    producerProps[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = "io.confluent.kafka.serializers.KafkaAvroSerializer"
    producerProps[KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = "mock://localhost"
    return DefaultKafkaProducerFactory<Long, JournalfoeringHendelseRecord>(producerProps)
        .createProducer()
}

fun Producer<Long, JournalfoeringHendelseRecord>.leggPåTopic(hendelse: JournalfoeringHendelseRecord): RecordMetadata {
    return send(ProducerRecord(AAPEN_DOK_JOURNALFØRING_V1, hendelse)).get()
}

fun EmbeddedKafkaBroker.opprettDittnavConsumer(
    topics: List<String> = listOf(
        K9_DITTNAV_VARSEL_BESKJED_AIVEN
    ),
): Consumer<String, K9Beskjed> {
    val consumerProps = KafkaTestUtils.consumerProps("dittnav-consumer-${UUID.randomUUID()}", "true", this)
    consumerProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] =
        "org.apache.kafka.common.serialization.StringDeserializer"
    consumerProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] =
        "org.apache.kafka.common.serialization.StringDeserializer"

    val consumer = DefaultKafkaConsumerFactory<String, K9Beskjed>(consumerProps).createConsumer()
    consumer.subscribe(topics)
    return consumer
}

fun <K, V> EmbeddedKafkaBroker.opprettKafkaStringConsumer(groupId: String, topics: List<String>): Consumer<K, V> {
    val consumerProps = KafkaTestUtils.consumerProps(groupId, "true", this)
    consumerProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
    consumerProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
    consumerProps[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = 100_000

    val consumer = DefaultKafkaConsumerFactory<K, V>(HashMap(consumerProps)).createConsumer()
    consumer.subscribe(topics)
    return consumer
}

fun Consumer<String, K9Beskjed>.lesMelding(
    søknadId: String,
    topic: String = K9_DITTNAV_VARSEL_BESKJED_AIVEN,
    maxWaitInSeconds: Long = 20
): ConsumerRecord<String, K9Beskjed> {

    val end = System.currentTimeMillis() + Duration.ofSeconds(maxWaitInSeconds).toMillis()
    seekToBeginning(assignment())
    while (System.currentTimeMillis() < end) {

        val entries: List<ConsumerRecord<String, K9Beskjed>> = poll(Duration.ofSeconds(10))
            .records(topic)
            .filter { it.key() == søknadId }

        if (entries.isNotEmpty()) {
            assertEquals(1, entries.size)
            return entries.first()
        }
    }
    throw IllegalStateException("Fant ikke dittnav varsel for søknad med id=$søknadId etter $maxWaitInSeconds sekunder.")
}

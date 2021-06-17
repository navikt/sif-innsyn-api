package no.nav.sifinnsynapi.config.kafka

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import java.time.Duration

@Configuration
internal class JoarkKafkaConfig(
    private val kafkaClusterProperties: KafkaClusterProperties
) {
    @Bean
    fun joarkConsumerFactory(): ConsumerFactory<String, String> {
        val consumerProps = kafkaClusterProperties.onprem.consumer
        return DefaultKafkaConsumerFactory(
            mutableMapOf<String, Any>(
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to consumerProps.enableAutoCommit,
                ConsumerConfig.GROUP_ID_CONFIG to consumerProps.groupId,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to consumerProps.autoOffsetReset,
                ConsumerConfig.ISOLATION_LEVEL_CONFIG to consumerProps.isolationLevel,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to consumerProps.keyDeserializer,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to "io.confluent.kafka.serializers.KafkaAvroDeserializer",
                "schema.registry.url" to consumerProps.schemaRegistryUrl
            ) + CommonKafkaConfig.commonConfig(kafkaClusterProperties.onprem)
        )
    }

    @Bean
    fun dokJournalføringKafkaJsonListenerContainerFactor(joarkConsumerFactory: ConsumerFactory<String, String>) =
        ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            this.consumerFactory = joarkConsumerFactory

            // https://docs.spring.io/spring-kafka/reference/html/#listener-container
            containerProperties.authorizationExceptionRetryInterval = Duration.ofSeconds(10L)

            // https://docs.spring.io/spring-kafka/docs/2.5.2.RELEASE/reference/html/#delivery-header
            containerProperties.isDeliveryAttemptHeader = true

            // https://docs.spring.io/spring-kafka/docs/2.5.2.RELEASE/reference/html/#committing-offsets
            containerProperties.ackMode = ContainerProperties.AckMode.RECORD;

            // https://docs.spring.io/spring-kafka/docs/2.5.2.RELEASE/reference/html/#exactly-once
            containerProperties.eosMode = ContainerProperties.EOSMode.BETA
        }
}

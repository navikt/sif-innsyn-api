package no.nav.sifinnsynapi.config.kafka

import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import no.nav.sifinnsynapi.config.kafka.CommonKafkaConfig.Companion.defaultRecoverer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DefaultAfterRollbackProcessor
import org.springframework.util.backoff.FixedBackOff
import java.time.Duration

@Configuration
internal class JoarkKafkaConfig(
    private val kafkaClusterProperties: KafkaClusterProperties
) {
    companion object {
        private val logger = LoggerFactory.getLogger(JoarkKafkaConfig::class.java)

        const val TEMA_NYTT_OMS = "OMS"
        const val MOTTAKS_KANAL_NAV_NO = "NAV_NO"
        const val ENDELIG_JOURNALFØRT = "EndeligJournalført"
    }

    @Bean
    fun joarkConsumerFactory(): DefaultKafkaConsumerFactory<Long, JournalfoeringHendelseRecord> {
        val consumerProps = kafkaClusterProperties.onprem.consumer
        return DefaultKafkaConsumerFactory(
            mutableMapOf<String, Any>(
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to consumerProps.enableAutoCommit,
                ConsumerConfig.GROUP_ID_CONFIG to consumerProps.groupId,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to consumerProps.autoOffsetReset,
                ConsumerConfig.ISOLATION_LEVEL_CONFIG to consumerProps.isolationLevel,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to consumerProps.keyDeserializer,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to "io.confluent.kafka.serializers.KafkaAvroDeserializer",
                KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG to consumerProps.schemaRegistryUrl,
                KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to "true"
            ) + CommonKafkaConfig.commonConfig(kafkaClusterProperties.onprem)
        )
    }

    @Bean
    fun dokJournalføringKafkaJsonListenerContainerFactor(joarkConsumerFactory: ConsumerFactory<Long, JournalfoeringHendelseRecord>) =
        ConcurrentKafkaListenerContainerFactory<Long, JournalfoeringHendelseRecord>().apply {
            this.consumerFactory = joarkConsumerFactory

            // https://docs.spring.io/spring-kafka/reference/html/#listener-container
            containerProperties.authorizationExceptionRetryInterval = Duration.ofSeconds(10L)

            // https://docs.spring.io/spring-kafka/docs/2.5.2.RELEASE/reference/html/#delivery-header
            containerProperties.isDeliveryAttemptHeader = true

            // https://docs.spring.io/spring-kafka/docs/2.5.2.RELEASE/reference/html/#committing-offsets
            containerProperties.ackMode = ContainerProperties.AckMode.RECORD;

            // https://docs.spring.io/spring-kafka/docs/2.5.2.RELEASE/reference/html/#exactly-once
            containerProperties.eosMode = ContainerProperties.EOSMode.BETA

            setRecordFilterStrategy {
                val hendelse = it.value()
                when {
                    hendelse.temaNytt == TEMA_NYTT_OMS && hendelse.mottaksKanal == MOTTAKS_KANAL_NAV_NO && hendelse.hendelsesType == ENDELIG_JOURNALFØRT -> false
                    else -> true
                }
            }

            setAfterRollbackProcessor(
                defaultAfterRollbackProsessor(
                    logger,
                    kafkaClusterProperties.onprem.consumer.retryInterval
                )
            )
        }

    private fun defaultAfterRollbackProsessor(logger: Logger, retryInterval: Long) =
        DefaultAfterRollbackProcessor<Long, JournalfoeringHendelseRecord>(
            defaultRecoverer(logger), FixedBackOff(retryInterval, Long.MAX_VALUE)
        ).apply {
            setClassifications(mapOf(), true)
        }
}

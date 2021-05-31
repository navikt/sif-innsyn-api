package no.nav.sifinnsynapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.sifinnsynapi.config.CommonKafkaConfig.Companion.configureConcurrentKafkaListenerContainerFactory
import no.nav.sifinnsynapi.soknad.SøknadRepository
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.transaction.ChainedTransactionManager
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.*


@Configuration
class OnpremKafkaConfig(
    private val objectMapper: ObjectMapper,
    private val søknadRepository: SøknadRepository,
    @Value("\${kafka.onprem.servers}") private val bootstrapServers: String,
    @Value("\${kafka.onprem.properties.security.protocol}") private val securityProtocol: String? = null,
    @Value("\${kafka.onprem.properties.sasl.mechanism}") private val saslMechanism: String? = null,
    @Value("\${kafka.onprem.properties.sasl.jaas-config}") private val jaasConfig: String? = null,
    @Value("\${kafka.onprem.properties.ssl.trust-store-location}") private val trustStoreLocation: String? = null,
    @Value("\${kafka.onprem.properties.ssl.trust-store-password}") private val trustStorePassword: String? = null,
    @Value("\${kafka.onprem.consumer.enable-auto-commit}") private val enableAutoCommit: Boolean,
    @Value("\${kafka.onprem.consumer.group-id}") private val groupId: String,
    @Value("\${kafka.onprem.consumer.auto-offset-reset}") private val autoOffsetReset: String,
    @Value("\${kafka.onprem.consumer.isolation-level}") private val isolationLevel: String,
    @Value("\${kafka.onprem.consumer.retry-interval}") private val retryInterval: Long,
    @Value("\${kafka.onprem.consumer.key-deserializer}") private val keyDeserializer: String,
    @Value("\${kafka.onprem.consumer.value-deserializer}") private val valueDeserializer: String,
    @Value("\${kafka.onprem.producer.client-id}") private val clientId: String,
    @Value("\${kafka.onprem.producer.key-serializer}") private val keySerializer: String,
    @Value("\${kafka.onprem.producer.value-serializer}") private val valueSerializer: String,
    @Value("\${kafka.onprem.producer.transaction-id-prefix}") private val transactionIdPrefix: String
) {

    companion object {
        private val logger = LoggerFactory.getLogger(OnpremKafkaConfig::class.java)
    }

    /*fun commonConfig() = mutableMapOf<String, Any>().apply {
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    } + securityConfig()

    fun securityConfig() = mutableMapOf<String, Any>().apply {
        securityProtocol?.let { put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, it) }
        saslMechanism?.let { put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, it) }
        jaasConfig?.let { put(SaslConfigs.SASL_JAAS_CONFIG, it) }
        trustStoreLocation?.let { put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, it) }
        trustStorePassword?.let { put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, it) }
    }

    @Bean
    fun onpremConsumerFactory(): ConsumerFactory<String, String> {
        private val consumerProperties = mutableMapOf<String, Any>(
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to enableAutoCommit,
            ConsumerConfig.GROUP_ID_CONFIG to groupId,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to autoOffsetReset,
            ConsumerConfig.ISOLATION_LEVEL_CONFIG to isolationLevel,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to keyDeserializer,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to valueDeserializer
        ) + commonConfig()

        return DefaultKafkaConsumerFactory(consumerProperties)
    }

    @Bean
    fun onpremProducerFactory(): ProducerFactory<String, String> {
        private val producerProperties = mutableMapOf<String, Any>(
            ProducerConfig.CLIENT_ID_CONFIG to clientId,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to keySerializer,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to valueSerializer
        ) + commonConfig()

        private val factory = DefaultKafkaProducerFactory<String, String>(producerProperties)
        factory.setTransactionIdPrefix("transactionIdPrefix")
        return factory
    }

    @Bean
    fun onpremKafkaTemplate(onpremProducerFactory: ProducerFactory<String, String>): KafkaTemplate<String, String> {
        return KafkaTemplate(onpremProducerFactory)
    }

    @Bean
    fun onpremKafkaJsonListenerContainerFactory(
        onpremConsumerFactory: ConsumerFactory<String, String>,
        chainedTransactionManager: ChainedTransactionManager,
        onpremKafkaTemplate: KafkaTemplate<String, String>
    ): ConcurrentKafkaListenerContainerFactory<String, String> = configureConcurrentKafkaListenerContainerFactory(
        clientId = "groupId",
        consumerFactory = onpremConsumerFactory,
        chainedTransactionManager = chainedTransactionManager,
        kafkaTemplate = onpremKafkaTemplate,
        retryInterprivate val = retryInterval,
        objectMapper = objectMapper,
        søknadRepository = søknadRepository,
        logger = logger
    )*/
}

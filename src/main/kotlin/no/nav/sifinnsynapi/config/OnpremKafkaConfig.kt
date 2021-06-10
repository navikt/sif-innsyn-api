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
import org.springframework.core.io.Resource
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.*
import org.springframework.kafka.transaction.KafkaTransactionManager


@Configuration
class OnpremKafkaConfig(
    private val objectMapper: ObjectMapper,
    private val søknadRepository: SøknadRepository,
    @Value("\${kafka.onprem.servers}") private val bootstrapServers: String,
    @Value("\${kafka.onprem.properties.security.protocol:#{null}}") private val securityProtocol: String?,
    @Value("\${kafka.onprem.properties.sasl.mechanism:#{null}}") private val saslMechanism: String?,
    @Value("\${kafka.onprem.properties.sasl.jaas-config:#{null}}") private val jaasConfig: String?,
    @Value("\${kafka.onprem.properties.ssl.truststore-location:#{null}}") private val trustStoreLocation: Resource?,
    @Value("\${kafka.onprem.properties.ssl.truststore-password:#{null}}") private val trustStorePassword: String?,
    @Value("\${kafka.onprem.properties.ssl.truststore-type:#{null}}") private val trustStoreType: String?,
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
    @Value("\${kafka.onprem.producer.transaction-id-prefix}") private val transactionIdPrefix: String,
    @Value("\${kafka.onprem.producer.retries}") private val retries: Int
) {

    companion object {
        private val logger = LoggerFactory.getLogger(OnpremKafkaConfig::class.java)
    }

    fun commonConfig() = mutableMapOf<String, Any>().apply {
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    } + securityConfig()

    fun securityConfig() = mutableMapOf<String, Any>().apply {
        securityProtocol?.let { put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, it) }
        saslMechanism?.let { put(SaslConfigs.SASL_MECHANISM, it) }
        jaasConfig?.let { put(SaslConfigs.SASL_JAAS_CONFIG, it) }
        trustStoreLocation?.let { put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, it.file.absolutePath) }
        trustStorePassword?.let { put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, it) }
        trustStoreType?.let { put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, it) }
    }

    @Bean
    fun onpremConsumerFactory(): ConsumerFactory<String, String> {
        val consumerProperties = mutableMapOf<String, Any>(
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
        val producerProperties = mutableMapOf<String, Any>(
            ProducerConfig.CLIENT_ID_CONFIG to clientId,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to keySerializer,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to valueSerializer,
            ProducerConfig.RETRIES_CONFIG to retries
        ) + commonConfig()

        val factory = DefaultKafkaProducerFactory<String, String>(producerProperties)
        factory.setTransactionIdPrefix(transactionIdPrefix)
        return factory
    }

    @Bean
    fun onpremKafkaTemplate(onpremProducerFactory: ProducerFactory<String, String>) =
        KafkaTemplate(onpremProducerFactory).apply {
            transactionIdPrefix = transactionIdPrefix
        }

    @Bean
    fun onpremKafkaTransactionManager(onpremProducerFactory: ProducerFactory<String, String>) =
        KafkaTransactionManager(onpremProducerFactory).apply {
            setTransactionIdPrefix(transactionIdPrefix)
        }

    @Bean
    fun onpremKafkaJsonListenerContainerFactory(
        onpremConsumerFactory: ConsumerFactory<String, String>,
        onpremKafkaTemplate: KafkaTemplate<String, String>,
        onpremKafkaTransactionManager: KafkaTransactionManager<String, String>,
    ): ConcurrentKafkaListenerContainerFactory<String, String> = configureConcurrentKafkaListenerContainerFactory(
        clientId = groupId,
        consumerFactory = onpremConsumerFactory,
        transactionManager = onpremKafkaTransactionManager,
        kafkaTemplate = onpremKafkaTemplate,
        retryInterval = retryInterval,
        objectMapper = objectMapper,
        søknadRepository = søknadRepository,
        logger = logger
    )
}

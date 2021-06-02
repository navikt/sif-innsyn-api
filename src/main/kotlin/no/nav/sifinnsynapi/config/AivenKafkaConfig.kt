package no.nav.sifinnsynapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.sifinnsynapi.config.CommonKafkaConfig.Companion.configureConcurrentKafkaListenerContainerFactory
import no.nav.sifinnsynapi.soknad.SøknadRepository
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import org.springframework.data.transaction.ChainedTransactionManager
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.*


@Configuration
class AivenKafkaConfig(
    private val objectMapper: ObjectMapper,
    private val søknadRepository: SøknadRepository,
    @Value("\${kafka.aiven.servers}") private val bootstrapServers: String,
    @Value("\${kafka.aiven.properties.security.protocol:#{null}}") private val securityProtocol: String?,
    @Value("\${kafka.aiven.properties.ssl.truststore-location:#{null}}") private val trustStoreLocation: Resource?,
    @Value("\${kafka.aiven.properties.ssl.truststore-password:#{null}}") private val trustStorePassword: String?,
    @Value("\${kafka.aiven.properties.ssl.truststore-type:#{null}}") private val trustStoreType: String?,
    @Value("\${kafka.aiven.properties.ssl.keystore-location:#{null}}") private val keyStoreLocation: Resource?,
    @Value("\${kafka.aiven.properties.ssl.keystore-password:#{null}}") private val keystorePassword: String?,
    @Value("\${kafka.aiven.properties.ssl.keystore-type:#{null}}") private val keystoreType: String?,
    @Value("\${kafka.aiven.consumer.enable-auto-commit}") private val enableAutoCommit: Boolean,
    @Value("\${kafka.aiven.consumer.group-id}") private val groupId: String,
    @Value("\${kafka.aiven.consumer.auto-offset-reset}") private val autoOffsetReset: String,
    @Value("\${kafka.aiven.consumer.isolation-level}") private val isolationLevel: String,
    @Value("\${kafka.aiven.consumer.retry-interval}") private val retryInterval: Long,
    @Value("\${kafka.aiven.consumer.key-deserializer}") private val keyDeserializer: String,
    @Value("\${kafka.aiven.consumer.value-deserializer}") private val valueDeserializer: String,
    @Value("\${kafka.aiven.producer.client-id}") private val clientId: String,
    @Value("\${kafka.aiven.producer.key-serializer}") private val keySerializer: String,
    @Value("\${kafka.aiven.producer.value-serializer}") private val valueSerializer: String,
    @Value("\${kafka.aiven.producer.transaction-id-prefix}") private val transactionIdPrefix: String,
    @Value("\${kafka.aiven.producer.retries}") private val retries: Int
) {

    companion object {
        private val logger = LoggerFactory.getLogger(AivenKafkaConfig::class.java)
    }

    fun commonConfig() = mutableMapOf<String, Any>().apply {
        put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    } + securityConfig()

    fun securityConfig() = mutableMapOf<String, Any>().apply {
        securityProtocol?.let { put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, it) }
        trustStoreLocation?.let { put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, it.file.absolutePath) }
        trustStorePassword?.let {  put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, it)  }
        trustStoreType?.let { put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, it) }
        keyStoreLocation?.let { put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, it.file.absolutePath) }
        keystorePassword?.let { put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, it) }
        keystoreType?.let { put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, it) }
    }

    @Bean
    fun aivenConsumerFactory(): ConsumerFactory<String, String> {
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
    fun aivenProducerFactory(): ProducerFactory<String, String> {
        val producerProperties = mutableMapOf<String, Any>(
            ProducerConfig.CLIENT_ID_CONFIG to clientId,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to keySerializer,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to valueSerializer,
            ProducerConfig.RETRIES_CONFIG to retries,
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true
        ) + commonConfig()

        val factory = DefaultKafkaProducerFactory<String, String>(producerProperties)
        factory.transactionCapable()
        factory.setTransactionIdPrefix(transactionIdPrefix)
        return factory
    }

    @Bean
    fun aivenKafkaTemplate(aivenProducerFactory: ProducerFactory<String, String>) = KafkaTemplate(aivenProducerFactory)

    @Bean
    fun aivenKafkaJsonListenerContainerFactory(
        aivenConsumerFactory: ConsumerFactory<String, String>,
        aivenTM: ChainedTransactionManager,
        aivenKafkaTemplate: KafkaTemplate<String, String>
    ): ConcurrentKafkaListenerContainerFactory<String, String> = configureConcurrentKafkaListenerContainerFactory(
        clientId = groupId,
        consumerFactory = aivenConsumerFactory,
        chainedTransactionManager = aivenTM,
        kafkaTemplate = aivenKafkaTemplate,
        retryInterval = retryInterval,
        objectMapper = objectMapper,
        søknadRepository = søknadRepository,
        logger = logger
    )
}

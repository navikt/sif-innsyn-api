package no.nav.sifinnsynapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.sifinnsynapi.common.AktørId
import no.nav.sifinnsynapi.common.TopicEntry
import no.nav.sifinnsynapi.soknad.SøknadRepository
import no.nav.sifinnsynapi.util.Constants
import no.nav.sifinnsynapi.util.MDCUtil
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.transaction.ChainedTransactionManager
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DefaultAfterRollbackProcessor
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.kafka.support.converter.JsonMessageConverter
import org.springframework.util.backoff.FixedBackOff
import java.nio.ByteBuffer
import java.time.Duration
import java.util.function.BiConsumer


@Configuration
class KafkaConfig(
        @Value("\${spring.kafka.consumer.retry-interval}")
        val retryInterval: Long,
        @Suppress("SpringJavaInjectionPointsAutowiringInspection") val kafkaTemplate: KafkaTemplate<String, Any>,
        val objectMapper: ObjectMapper,
        val søknadRepository: SøknadRepository,
        @Value("\${spring.application.name:sif-innsyn-api}") private val applicationName: String
) {
    companion object {
        private val logger = LoggerFactory.getLogger(KafkaConfig::class.java)
    }

    @Bean
    fun kafkaJsonListenerContainerFactory(
            @Suppress("SpringJavaInjectionPointsAutowiringInspection") consumerFactory: ConsumerFactory<String, String>,
            chainedTransactionManager: ChainedTransactionManager
    ): KafkaListenerContainerFactory<*> {

        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()

        factory.consumerFactory = consumerFactory

        factory.setReplyTemplate(kafkaTemplate)

        // https://docs.spring.io/spring-kafka/docs/2.5.2.RELEASE/reference/html/#payload-conversion-with-batch
        factory.setMessageConverter(JsonMessageConverter(objectMapper))

        // https://docs.spring.io/spring-kafka/docs/2.5.2.RELEASE/reference/html/#filtering-messages
        factory.setRecordFilterStrategy {
            val antallForsøk = ByteBuffer.wrap(it.headers()
                    .lastHeader(KafkaHeaders.DELIVERY_ATTEMPT).value())
                    .int

            if (antallForsøk > 1) logger.warn("Konsumering av ${it.topic()}-${it.partition()} med offset ${it.offset()} feilet første gang. Prøver for $antallForsøk gang.")

            val topicEntry = objectMapper.readValue(it.value(), TopicEntry::class.java).data
            val correlationId = topicEntry.metadata.correlationId
            MDCUtil.toMDC(Constants.NAV_CALL_ID, correlationId)
            MDCUtil.toMDC(Constants.NAV_CONSUMER_ID, applicationName)

            val søker = JSONObject(topicEntry.melding).getJSONObject("søker")
            when (søknadRepository.existsSøknadDAOByAktørIdAndJournalpostId(AktørId(søker.getString("aktørId")), topicEntry.journalførtMelding.journalpostId)) {
                true -> {
                    logger.info("Fant duplikat, skipper deserialisering")
                    true
                }
                false -> {
                    logger.info("Fant IKKE duplikat, deserialiserer")
                    false
                }
            }
        }

        // https://docs.spring.io/spring-kafka/docs/2.5.2.RELEASE/reference/html/#chained-transaction-manager
        factory.containerProperties.transactionManager = chainedTransactionManager

        // https://docs.spring.io/spring-kafka/docs/2.5.2.RELEASE/reference/html/#exactly-once
        factory.containerProperties.eosMode = ContainerProperties.EOSMode.BETA

        // https://docs.spring.io/spring-kafka/docs/2.5.2.RELEASE/reference/html/#committing-offsets
        factory.containerProperties.ackMode = ContainerProperties.AckMode.RECORD;

        // https://docs.spring.io/spring-kafka/docs/2.5.2.RELEASE/reference/html/#delivery-header
        factory.containerProperties.isDeliveryAttemptHeader = true

        // https://docs.spring.io/spring-kafka/reference/html/#listener-container
        factory.containerProperties.authorizationExceptionRetryInterval = Duration.ofSeconds(10L)

        //https://docs.spring.io/spring-kafka/docs/2.5.2.RELEASE/reference/html/#after-rollback
        val defaultAfterRollbackProcessor = DefaultAfterRollbackProcessor<String, String>(recoverer(), FixedBackOff(retryInterval, Long.MAX_VALUE))
        defaultAfterRollbackProcessor.setClassifications(mapOf(), true)
        factory.setAfterRollbackProcessor(defaultAfterRollbackProcessor)

        return factory
    }

    private fun recoverer() = BiConsumer { cr: ConsumerRecord<*, *>, ex: Exception ->
        logger.error("Retry attempts exhausted for ${cr.topic()}-${cr.partition()}@${cr.offset()}", ex)
    }
}

package no.nav.sifinnsynapi.config.kafka

import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import no.nav.sifinnsynapi.config.kafka.CommonKafkaConfig.Companion.defaultRecoverer
import no.nav.sifinnsynapi.soknad.SøknadService
import no.nav.sifinnsynapi.util.Constants
import no.nav.sifinnsynapi.util.MDCUtil
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.SeekToCurrentErrorHandler
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.util.backoff.FixedBackOff
import java.nio.ByteBuffer
import java.time.Duration

@Configuration
internal class JoarkKafkaConfig(
    private val kafkaClusterProperties: KafkaClusterProperties,
    private val søknadService: SøknadService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(JoarkKafkaConfig::class.java)

        const val TEMA_NYTT_OMS = "oms"
        const val ENDELIG_JOURNALFØRT = "endeligjournalført"
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
    fun joarkKafkaJsonListenerContainerFactor(joarkConsumerFactory: ConsumerFactory<Long, JournalfoeringHendelseRecord>) =
        ConcurrentKafkaListenerContainerFactory<Long, JournalfoeringHendelseRecord>().apply {
            consumerFactory = joarkConsumerFactory

            // https://docs.spring.io/spring-kafka/reference/html/#listener-container
            containerProperties.authorizationExceptionRetryInterval = Duration.ofSeconds(10L)

            // https://docs.spring.io/spring-kafka/docs/2.5.2.RELEASE/reference/html/#delivery-header
            containerProperties.isDeliveryAttemptHeader = true

            // https://docs.spring.io/spring-kafka/docs/2.5.2.RELEASE/reference/html/#committing-offsets
            containerProperties.ackMode = ContainerProperties.AckMode.RECORD;

            // https://docs.spring.io/spring-kafka/docs/2.5.2.RELEASE/reference/html/#exactly-once
            containerProperties.eosMode = ContainerProperties.EOSMode.BETA

            // https://docs.spring.io/spring-kafka/reference/html/#seek-to-current
            setErrorHandler(
                SeekToCurrentErrorHandler(
                    defaultRecoverer(logger),
                    FixedBackOff(kafkaClusterProperties.onprem.consumer.retryInterval, Long.MAX_VALUE)
                )
            )

            setRecordFilterStrategy {
                MDCUtil.clearFomMDC(Constants.JOURNALPOST_ID)
                MDCUtil.clearFomMDC(Constants.K9_SAK_ID)
                loggAntallForsøk(it)

                val journalføringsHendelse = it.value()
                when {
                    journalføringsHendelse.erRelevant() && søknadEksisterer(journalføringsHendelse) -> {
                        MDCUtil.toMDC(Constants.JOURNALPOST_ID, journalføringsHendelse.journalpostId)
                        false
                    }
                    else -> true
                }
            }
        }

    private fun søknadEksisterer(journalføringsHendelse: JournalfoeringHendelseRecord) =
        søknadService.søknadGittJournalpostIdEksisterer("${journalføringsHendelse.journalpostId}")

    private fun loggAntallForsøk(it: ConsumerRecord<Long, JournalfoeringHendelseRecord>) {
        val antallForsøk = ByteBuffer.wrap(
            it.headers()
                .lastHeader(KafkaHeaders.DELIVERY_ATTEMPT).value()
        )
            .int

        if (antallForsøk > 1) logger.warn("Konsumering av ${it.topic()}-${it.partition()} med offset ${it.offset()} feilet første gang. Prøver for $antallForsøk gang.")
    }

    private fun JournalfoeringHendelseRecord.erRelevant(): Boolean =
        temaNytt.lowercase() == TEMA_NYTT_OMS && hendelsesType.lowercase() == ENDELIG_JOURNALFØRT
}

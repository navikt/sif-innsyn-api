package no.nav.sifinnsynapi.config.kafka

import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import no.nav.sifinnsynapi.config.kafka.CommonKafkaConfig.Companion.defaultRecoverer
import no.nav.sifinnsynapi.soknad.SøknadService
import no.nav.sifinnsynapi.util.MDCConstants.CORRELATION_ID
import no.nav.sifinnsynapi.util.MDCConstants.JOURNALPOST_ID
import no.nav.sifinnsynapi.util.MDCConstants.K9_SAK_ID
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
import org.springframework.kafka.listener.DefaultErrorHandler
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
        val consumerProps = kafkaClusterProperties.aiven.consumer
        return DefaultKafkaConsumerFactory(
            mutableMapOf<String, Any>(
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to consumerProps.enableAutoCommit,
                ConsumerConfig.GROUP_ID_CONFIG to consumerProps.groupId,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to consumerProps.autoOffsetReset,
                ConsumerConfig.ISOLATION_LEVEL_CONFIG to consumerProps.isolationLevel,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to consumerProps.keyDeserializer,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to "io.confluent.kafka.serializers.KafkaAvroDeserializer",
                KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG to consumerProps.schemaRegistryUrl,
                KafkaAvroDeserializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE to "USER_INFO",
                KafkaAvroDeserializerConfig.USER_INFO_CONFIG to "${consumerProps.schemaRegistryUser}:${consumerProps.schemaRegistryPassword}",
                KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG to "true"
            ) + CommonKafkaConfig.commonConfig(kafkaClusterProperties.aiven)
        )
    }

    @Bean
    fun joarkKafkaJsonListenerContainerFactor(joarkConsumerFactory: ConsumerFactory<Long, JournalfoeringHendelseRecord>) =
        ConcurrentKafkaListenerContainerFactory<Long, JournalfoeringHendelseRecord>().apply {
            consumerFactory = joarkConsumerFactory

            // https://docs.spring.io/spring-kafka/reference/html/#listener-container
            containerProperties.setAuthExceptionRetryInterval(Duration.ofSeconds(10L))

            // https://docs.spring.io/spring-kafka/docs/2.5.2.RELEASE/reference/html/#delivery-header
            containerProperties.isDeliveryAttemptHeader = true

            // https://docs.spring.io/spring-kafka/docs/2.5.2.RELEASE/reference/html/#committing-offsets
            containerProperties.ackMode = ContainerProperties.AckMode.RECORD;

            // https://docs.spring.io/spring-kafka/docs/2.5.2.RELEASE/reference/html/#exactly-once
            containerProperties.eosMode = ContainerProperties.EOSMode.V2

            // https://docs.spring.io/spring-kafka/reference/html/#seek-to-current
            setCommonErrorHandler(
                DefaultErrorHandler(
                    defaultRecoverer(logger),
                    FixedBackOff(kafkaClusterProperties.aiven.consumer.retryInterval, Long.MAX_VALUE)
                )
            )

            setRecordFilterStrategy {
                MDCUtil.clearFomMDC(JOURNALPOST_ID)
                MDCUtil.clearFomMDC(K9_SAK_ID)
                MDCUtil.clearFomMDC(CORRELATION_ID)
                loggAntallForsøk(it)

                val journalføringsHendelse = it.value()
                when {
                    journalføringsHendelse.erRelevant() && søknadEksisterer(journalføringsHendelse) -> {
                        MDCUtil.toMDC(JOURNALPOST_ID, journalføringsHendelse.journalpostId)
                        MDCUtil.toMDC(CORRELATION_ID, MDCUtil.callIdOrNew())
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

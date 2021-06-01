package no.nav.sifinnsynapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.transaction.ChainedTransactionManager
import org.springframework.kafka.annotation.KafkaListenerConfigurer
import org.springframework.kafka.config.KafkaListenerEndpointRegistrar
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.transaction.KafkaTransactionManager
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean

@Configuration
class TxConfiguration(
    private val validator: LocalValidatorFactoryBean
) : KafkaListenerConfigurer {

    @Primary
    @Bean(name = [TM])
    fun chainedTM(
        onpremJpaTM: JpaTransactionManager,
        onpremKafkaTM: KafkaTransactionManager<String, String>
    ): ChainedTransactionManager = ChainedTransactionManager(
        onpremKafkaTM,
        onpremJpaTM
    )

    @Bean(name = [AIVEN_TM])
    fun aivenChainedTM(
        aivenJpaTM: JpaTransactionManager,
        aivenKafkaTM: KafkaTransactionManager<String, String>
    ): ChainedTransactionManager = ChainedTransactionManager(
        aivenKafkaTM,
        aivenJpaTM
    )

    @Bean(name = [ONPREM_KAFKA_TM])
    fun onpremKafkaTM(onpremProducerFactory: ProducerFactory<String, String>): KafkaTransactionManager<String, String> {
        val tm = KafkaTransactionManager(onpremProducerFactory)
        tm.isNestedTransactionAllowed = true
        return tm
    }

    @Bean(name = [AIVEN_KAFKA_TM])
    fun aivenKafkaTM(aivenProducerFactory: ProducerFactory<String, String>): KafkaTransactionManager<String, String> {
        val tm = KafkaTransactionManager(aivenProducerFactory)
        tm.isNestedTransactionAllowed = true
        return tm
    }

    @Bean(name = [ONPREM_JPA_TM])
    fun onpremJpaTM(): JpaTransactionManager {
        return JpaTransactionManager()
    }

    @Bean(name = [AIVEN_JPA_TM])
    fun aivenJpaTM(): JpaTransactionManager {
        return JpaTransactionManager()
    }

    override fun configureKafkaListeners(registrar: KafkaListenerEndpointRegistrar) {
        registrar.validator = validator
    }

    companion object {
        const val TM = "transactionManager"
        const val AIVEN_TM = "aivenTM"
        const val ONPREM_KAFKA_TM = "onpremKafkaTM"
        const val ONPREM_JPA_TM = "onpremJpaTM"
        const val AIVEN_KAFKA_TM = "aivenKafkaTM"
        const val AIVEN_JPA_TM = "aivenJpaTM"
    }
}

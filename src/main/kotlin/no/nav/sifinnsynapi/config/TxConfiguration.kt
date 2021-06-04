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

    @Bean(name = [ONPREM_KAFKA_TM])
    fun onpremKafkaTM(onpremProducerFactory: ProducerFactory<String, String>): KafkaTransactionManager<String, String> {
        val tm = KafkaTransactionManager(onpremProducerFactory)
        tm.isNestedTransactionAllowed = true
        return tm
    }

    @Bean(name = [ONPREM_JPA_TM])
    fun onpremJpaTM(): JpaTransactionManager {
        return JpaTransactionManager()
    }

    override fun configureKafkaListeners(registrar: KafkaListenerEndpointRegistrar) {
        registrar.validator = validator
    }

    companion object {
        const val TM = "transactionManager"
        const val ONPREM_KAFKA_TM = "onpremKafkaTM"
        const val ONPREM_JPA_TM = "onpremJpaTM"
    }
}

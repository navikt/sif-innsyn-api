package no.nav.sifinnsynapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.transaction.ChainedTransactionManager
import org.springframework.kafka.annotation.KafkaListenerConfigurer
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaListenerEndpointRegistrar
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.kafka.support.converter.StringJsonMessageConverter
import org.springframework.kafka.transaction.KafkaTransactionManager
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean

/*@Configuration
class TxConfiguration(
        private val validator: LocalValidatorFactoryBean
) : KafkaListenerConfigurer {

    @Primary
    @Bean(name = ["transactionManager"])
    fun chainedTM(jpaTM: JpaTransactionManager,
                  kafkaTM: KafkaTransactionManager<Any, Any>): ChainedTransactionManager {
        return ChainedTransactionManager(kafkaTM, jpaTM)
    }

    @Bean(name = [KAFKA_TM])
    fun kafkaTM(pf: ProducerFactory<Any, Any>): KafkaTransactionManager<Any, Any> {
        val tm = KafkaTransactionManager(pf)
        tm.isNestedTransactionAllowed = true
        return tm
    }

    @Bean(name = [JPA_TM])
    fun jpaTM(): JpaTransactionManager {
        return JpaTransactionManager()
    }

    @Bean
    fun kafkaListenerContainerFactory(
            cf: ConsumerFactory<Any, Any>, tm: KafkaTransactionManager<Any, Any>, mapper: ObjectMapper): KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<Any, Any>> {
        val factory = ConcurrentKafkaListenerContainerFactory<Any, Any>()
        factory.consumerFactory = cf
        factory.setMessageConverter(StringJsonMessageConverter(mapper))
        factory.containerProperties.transactionManager = tm
        return factory
    }

    override fun configureKafkaListeners(registrar: KafkaListenerEndpointRegistrar) {
        registrar.validator = validator
    }

    companion object {
        const val KAFKA_TM = "kafkaTM"
        const val JPA_TM = "jpaTM"
    }
}*/

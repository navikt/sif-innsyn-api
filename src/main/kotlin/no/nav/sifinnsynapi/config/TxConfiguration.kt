package no.nav.sifinnsynapi.config

import jakarta.persistence.EntityManagerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.orm.jpa.JpaTransactionManager

@Configuration
class TxConfiguration {
    companion object {
        const val TRANSACTION_MANAGER = "transactionManager"
    }

    @Bean(TRANSACTION_MANAGER)
    @Primary
    fun transactionManager(entityManagerFactory: EntityManagerFactory): JpaTransactionManager {
        return JpaTransactionManager(entityManagerFactory)
    }
}

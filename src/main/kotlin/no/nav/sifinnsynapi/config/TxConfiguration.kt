package no.nav.sifinnsynapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import javax.sql.DataSource

@Configuration
class TxConfiguration {
    companion object {
        const val TRANSACTION_MANAGER = "transactionManager"
    }

    @Bean
    fun transactionManager(dataSource: DataSource): DataSourceTransactionManager {
        return DataSourceTransactionManager(dataSource)
    }
}

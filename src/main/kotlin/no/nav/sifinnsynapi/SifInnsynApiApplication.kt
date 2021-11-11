package no.nav.sifinnsynapi

import no.nav.sifinnsynapi.soknad.SøknadRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.transaction.annotation.EnableTransactionManagement

@SpringBootApplication(
    exclude = [
        ErrorMvcAutoConfiguration::class
    ]
)
@EnableRetry
@EnableKafka
@EnableTransactionManagement
@EnableScheduling
@ConfigurationPropertiesScan("no.nav.sifinnsynapi")
@EnableConfigurationProperties
class SifInnsynApiApplication(private val søknadRepository: SøknadRepository) : CommandLineRunner {

    private companion object {
        private val logger = LoggerFactory.getLogger(SifInnsynApiApplication::class.java)
    }

    // TODO: 11/11/2021 Slettes etter prodsetting
    /**
     * Callback used to run the bean.
     * @param args incoming main method arguments
     * @throws Exception on error
     */
    override fun run(vararg args: String?) {
        søknadRepository.findAllBySaksIdIsNotNull().map {
            logger.info("Resetter saksId på søknad med id {} til null.", it.id)
            val oppdatertSøknad = søknadRepository.save(it.copy(saksId = null))
            assert(oppdatertSøknad.saksId == null)
        }
    }
}

fun main(args: Array<String>) {
    runApplication<SifInnsynApiApplication>(*args)
}

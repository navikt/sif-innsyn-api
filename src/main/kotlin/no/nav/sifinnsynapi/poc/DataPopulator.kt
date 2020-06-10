package no.nav.sifinnsynapi.poc

import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class DataPopulator(
        private val repo: SøknadRepository
) {
    companion object {
        val log = LoggerFactory.getLogger(CommandLineRunner::class.java)
    }

    @Bean
    //@Profile("local")
    fun commandlineRunner(): CommandLineRunner {
        return CommandLineRunner {
            listOf(Søknad(
                    språk = "nb",
                    harForståttRettigheterOgPlikter = true,
                    harBekreftetOpplysninger = true,
                    beskrivelse = "korona",
                    søknadstype = "pleiepenger"
            ), Søknad(
                    språk = "nb",
                    harForståttRettigheterOgPlikter = true,
                    harBekreftetOpplysninger = true,
                    beskrivelse = "korona",
                    søknadstype = "omsorgspenger"
            ), Søknad(
                    språk = "nb",
                    harForståttRettigheterOgPlikter = true,
                    harBekreftetOpplysninger = true,
                    beskrivelse = "korona",
                    søknadstype = "koronapenger"
            )).map {
                log.info("Lagrer søknad: {}", it)
                repo.save(it)
            }.forEach {
                log.info("Hentet Søknad: {}", it)
            }
        }
    }
}

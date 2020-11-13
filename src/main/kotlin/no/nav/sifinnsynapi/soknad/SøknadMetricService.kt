package no.nav.sifinnsynapi.soknad

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class SøknadMetricService(
        private val søknadRepository: SøknadRepository,
        private val meterRegistry: MeterRegistry
) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(SøknadMetricService::class.java)
    }

    @Scheduled(fixedRateString = "#{'\${no.nav.metrics.interval.antall_brukere}'}")
    fun reportSøknadMetrics() {

        val antallBrukere = søknadRepository.finnAnntallUnikeSøkere()
        logger.info("Måling: antall unike brukere = $antallBrukere")

        Gauge.builder("sif_innsyn_antall_unike_brukere") { antallBrukere }
                .description("Måler for antall unike brukere i databasen")
                .register(meterRegistry)
    }
}

package no.nav.sifinnsynapi.soknad

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import no.nav.sifinnsynapi.common.Søknadstype
import no.nav.sifinnsynapi.k8s.LeaderService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class SøknadMetricService(
    private val søknadRepository: SøknadRepository,
    private val meterRegistry: MeterRegistry,
    private val leaderService: LeaderService
) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(SøknadMetricService::class.java)
    }

    @Scheduled(fixedRateString = "#{'\${no.nav.metrics.interval}'}")
    fun reportSøknadMetrics() {
        leaderService.executeAsLeader("reportSøknadMetrics") {
            søknadRepository.finnAntallUnikeSøkere().let {
                logger.info("Måling: antall unike brukere = $it")
                Gauge.builder("sif_innsyn_antall_unike_brukere") { it }
                    .description("Måler for antall unike brukere i databasen")
                    .register(meterRegistry)
            }

            søknadRepository.count().let {
                logger.info("Måling: antall søknader = $it")
                Gauge.builder("sif_innsyn_antall_soknader") { it }
                    .description("Måler for antall søknader i databasen")
                    .register(meterRegistry)
            }

            søknadRepository.finnAntallSøknaderGittSøknadstype(Søknadstype.PP_SYKT_BARN.name).let {
                logger.info("Måling: antall pleiepengesøknader = $it")
                Gauge.builder("sif_innsyn_antall_pleiepengesoknader") { it }
                    .description("Måler for antall pleiepengesøknader i databasen")
                    .register(meterRegistry)
            }
        }
    }
}

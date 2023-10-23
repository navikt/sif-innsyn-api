package no.nav.sifinnsynapi.mikrofrontend

import no.nav.sifinnsynapi.dittnav.MicrofrontendAction
import no.nav.sifinnsynapi.dittnav.MicrofrontendId
import no.nav.sifinnsynapi.k8s.LeaderService
import no.nav.sifinnsynapi.soknad.SøknadDAO
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.TimeUnit

@Component
class MikrofrontendScheduler(
    private val mikrofrontendService: MikrofrontendService,
    private val leaderService: LeaderService,
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(MikrofrontendScheduler::class.java)
        private const val BATCH_SIZE = 1
    }

    /**
     * Henter all aktiverte dine-pleiepenger mikrofrontender og deaktiverer dem.
     * Sender ut ditt nav varsel med om å deaktivere dine-pleiepenger.
     * Oppdaterer status på mikrofrontend entitet.
     */
    //@Scheduled(fixedDelay = Long.MAX_VALUE, initialDelay = 1000*5*60)
    fun deaktiverAlleDinePleiepengerMicrofrontend() = leaderService.executeAsLeader {
        logger.info("Deaktiverer alle mikrofrontend for pleiepengesøknader.")
        val eksiterendeStatus = MicrofrontendAction.ENABLE
        val statusÅOppdatere = MicrofrontendAction.DISABLE
        val mikrofrontendId = MicrofrontendId.PLEIEPENGER_INNSYN.id
        var batchNummer = 1
        var harMerÅLese = true

        while (harMerÅLese) {
            val mikrofrontendDAOS = mikrofrontendService.hentMikrofrontendIdAndStatus(
                mikrofrontendId = mikrofrontendId,
                status = eksiterendeStatus,
                limit = BATCH_SIZE
            )

            logger.info("Prosesserer batch nummer $batchNummer.")
            deaktiver(mikrofrontendDAOS, statusÅOppdatere)
            logger.info("Batch nummer $batchNummer ferdig.")
            batchNummer++

            if (mikrofrontendDAOS.isEmpty()) {
                harMerÅLese = false
            }
        }
        logger.info("Deaktivering av alle mikrofrontend for pleiepengesøknader fullført.")
    }

    @Scheduled(fixedDelay = 20, initialDelay = 5, timeUnit = TimeUnit.MINUTES)
    fun aktiverMikrofrontendForPleiepengesøknaderDeSisteSeksMåneder() = leaderService.executeAsLeader {
        logger.info("Aktiverer mikrofrontend for pleiepengesøknader de siste seks måneder.")
        var batchNummer = 1
        var harMerÅLese = true
        val statusÅOppdatere = MicrofrontendAction.ENABLE

        while (harMerÅLese) {
            val søknader = mikrofrontendService.finnUnikeSøknaderUtenMikrofrontendSisteSeksMåneder(limit = BATCH_SIZE)
            aktiver(søknader, statusÅOppdatere, batchNummer)
            if (søknader.isEmpty()) {
                harMerÅLese = false
            }
            batchNummer++
        }

        logger.info("Aktivering av mikrofrontend for pleiepengesøknader de siste seks måneder fullført.")
    }

    private fun deaktiver(
        mikrofrontendDAOS: List<MikrofrontendDAO>,
        statusÅOppdatere: MicrofrontendAction,
    ) {
        mikrofrontendDAOS.forEach { mikrofrontendDAO ->
            runCatching {
                mikrofrontendService.sendOgLagre(
                    mikrofrontendDAO = mikrofrontendDAO.copy(status = statusÅOppdatere),
                    microfrontendAction = statusÅOppdatere
                )
            }.onFailure {
                logger.error("Feilet med å deaktivere dine-pleiepenger mikrofrontend. Prøver igjen senere.", it)
            }
        }
    }

    private fun aktiver(
        søknader: List<SøknadDAO>,
        statusÅOppdatere: MicrofrontendAction,
        batchNummer: Int,
    ) {
        logger.info("Prosesserer batch ${batchNummer}.")
        søknader
            .map { it.toMicrofrontendDAO(statusÅOppdatere) }
            .forEach { mikrofrontendDAO: MikrofrontendDAO ->
                runCatching {
                    mikrofrontendService.sendOgLagre(mikrofrontendDAO, statusÅOppdatere)
                }.onFailure {
                    logger.error("Feilet med å oppdatere MikrofrontendTabell. Prøver igjen senere.", it)
                }
            }
        logger.info("Batch $batchNummer} ferdig.")
    }

    private fun SøknadDAO.toMicrofrontendDAO(statusÅOppdatere: MicrofrontendAction) = MikrofrontendDAO(
        id = UUID.randomUUID(),
        fødselsnummer = fødselsnummer.fødselsnummer!!,
        mikrofrontendId = MicrofrontendId.PLEIEPENGER_INNSYN.id,
        status = statusÅOppdatere,
        opprettet = opprettet,
        behandlingsdato = null,
    )
}

package no.nav.sifinnsynapi.mikrofrontend

import no.nav.sifinnsynapi.common.Metadata
import no.nav.sifinnsynapi.dittnav.DittnavService
import no.nav.sifinnsynapi.dittnav.K9Microfrontend
import no.nav.sifinnsynapi.dittnav.MicrofrontendAction
import no.nav.sifinnsynapi.dittnav.MicrofrontendId
import no.nav.sifinnsynapi.dittnav.Sensitivitet
import no.nav.sifinnsynapi.k8s.LeaderService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.concurrent.TimeUnit

@Service
class MikrofrontendService(
    private val mikrofrontendRepository: MikrofrontendRepository,
    private val dittnavService: DittnavService,
    private val leaderService: LeaderService,
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(MikrofrontendService::class.java)
    }

    /**
     * Henter all deaktiverte dine-pleiepenger mikrofrontender og aktiverer dem.
     * Sender ut ditt nav varsel med om å aktivere dine-pleiepenger.
     * Oppdaterer status på mikrofrontend entitet.
     */
    @Scheduled(fixedDelay = 15, timeUnit = TimeUnit.MINUTES)
    @Transactional
    fun aktiverDinePleiepengerFrontend() {
        leaderService.executeAsLeader {
            val antallDeaktiverteDinePleiepenger = mikrofrontendRepository.countAllByMikrofrontendIdAndStatus(
                MicrofrontendId.PLEIEPENGER_INNSYN.id,
                MicrofrontendAction.DISABLE
            )
            logger.info("Fant ${antallDeaktiverteDinePleiepenger} deaktiverte dine-pleiepenger mikrofrontend")

            mikrofrontendRepository.findAllByMikrofrontendIdAndStatus(
                MicrofrontendId.PLEIEPENGER_INNSYN.id,
                MicrofrontendAction.DISABLE
            )
                .forEach { mikrofrontend: MikrofrontendDAO ->
                    runCatching {
                        dittnavService.toggleMicrofrontend(mikrofrontend.toK9Microfrontend())
                        mikrofrontendRepository.save(mikrofrontend.copy(status = MicrofrontendAction.ENABLE))
                    }.onFailure {
                        logger.error("Feilet med å aktivere dine-pleiepenger mikrofrontend. Prøver igjen senere.", it)
                    }
                }
        }
    }

    // TODO: Legg til jobb for å hente all pleiepengesøknader og oppdatere mikrofrontend tabell dersom søker ikke er oppført der.

    private fun MikrofrontendDAO.toK9Microfrontend() = K9Microfrontend(
        metadata = Metadata(version = 1, correlationId = UUID.randomUUID().toString()),
        ident = fødselsnummer,
        microfrontendId = MicrofrontendId.fromId(mikrofrontendId),
        action = MicrofrontendAction.ENABLE,
        sensitivitet = Sensitivitet.SUBSTANTIAL,
        initiatedBy = "sif-innsyn-api"
    )
}

package no.nav.sifinnsynapi.mikrofrontend

import no.nav.sifinnsynapi.common.Metadata
import no.nav.sifinnsynapi.dittnav.DittnavService
import no.nav.sifinnsynapi.dittnav.K9Microfrontend
import no.nav.sifinnsynapi.dittnav.MicrofrontendAction
import no.nav.sifinnsynapi.dittnav.MicrofrontendId
import no.nav.sifinnsynapi.dittnav.Sensitivitet
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import java.util.concurrent.TimeUnit

@Service
class MikrofrontendService(
    private val mikrofrontendRepository: MikrofrontendRepository,
    private val dittnavService: DittnavService,
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

    private fun MikrofrontendDAO.toK9Microfrontend() = K9Microfrontend(
        metadata = Metadata(version = 1, correlationId = UUID.randomUUID().toString()),
        ident = fødselsnummer,
        microfrontendId = MicrofrontendId.valueOf(mikrofrontendId),
        action = status,
        sensitivitet = Sensitivitet.SUBSTANTIAL,
        initiatedBy = "sif-innsyn-api"
    )
}

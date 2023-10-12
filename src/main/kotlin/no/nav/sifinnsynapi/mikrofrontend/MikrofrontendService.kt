package no.nav.sifinnsynapi.mikrofrontend

import no.nav.sifinnsynapi.common.Metadata
import no.nav.sifinnsynapi.common.Søknadstype
import no.nav.sifinnsynapi.dittnav.DittnavService
import no.nav.sifinnsynapi.dittnav.K9Microfrontend
import no.nav.sifinnsynapi.dittnav.MicrofrontendAction
import no.nav.sifinnsynapi.dittnav.MicrofrontendId
import no.nav.sifinnsynapi.dittnav.Sensitivitet
import no.nav.sifinnsynapi.k8s.LeaderService
import no.nav.sifinnsynapi.soknad.SøknadService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit

@Service
class MikrofrontendService(
    private val mikrofrontendRepository: MikrofrontendRepository,
    private val søknadService: SøknadService,
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
    fun aktiverDinePleiepengerFrontend() = leaderService.executeAsLeader {
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

    /**
     * Oppdaterer mikrofrontend-tabellen med nye søknader.
     *
     * Denne metoden kjører regelmessig hvert 15. minutt. Den henter alle søknader med unike fødselsnummer
     * for søknadstypen `PP_SYKT_BARN`. For hver unik søknad, sjekker den om fødselsnummeret allerede
     * eksisterer i mikrofrontend-tabellen. Hvis det ikke eksisterer, opprettes en ny `MikrofrontendDAO`
     * og lagres i databasen. Til slutt, en handling for å aktivere mikrofrontend blir trigget.
     */
    @Scheduled(fixedDelay = 20, timeUnit = TimeUnit.MINUTES)
    @Transactional
    fun oppdaterMikrofrontendTabell() = leaderService.executeAsLeader {
        søknadService.finnAlleSøknaderMedUnikeFødselsnummerForSøknadstype(Søknadstype.PP_SYKT_BARN)
            .filter { !mikrofrontendRepository.existsByFødselsnummer(it.fødselsnummer.fødselsnummer!!) }
            .map {
                MikrofrontendDAO(
                    id = UUID.randomUUID(),
                    fødselsnummer = it.fødselsnummer.fødselsnummer!!,
                    mikrofrontendId = MicrofrontendId.PLEIEPENGER_INNSYN.id,
                    status = MicrofrontendAction.ENABLE,
                    opprettet = ZonedDateTime.now(),
                    behandlingsdato = null,
                )
            }
            .forEach {
                dittnavService.toggleMicrofrontend(it.toK9Microfrontend())
                mikrofrontendRepository.save(it)
            }
    }

    private fun MikrofrontendDAO.toK9Microfrontend() = K9Microfrontend(
        metadata = Metadata(version = 1, correlationId = UUID.randomUUID().toString()),
        ident = fødselsnummer,
        microfrontendId = MicrofrontendId.fromId(mikrofrontendId),
        action = MicrofrontendAction.ENABLE,
        sensitivitet = Sensitivitet.SUBSTANTIAL,
        initiatedBy = "sif-innsyn-api"
    )
}

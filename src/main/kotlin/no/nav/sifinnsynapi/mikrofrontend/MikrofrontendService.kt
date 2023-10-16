package no.nav.sifinnsynapi.mikrofrontend

import no.nav.sifinnsynapi.common.Metadata
import no.nav.sifinnsynapi.common.Søknadstype
import no.nav.sifinnsynapi.config.TxConfiguration
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
import org.springframework.transaction.annotation.Propagation
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
     * Henter all aktiverte dine-pleiepenger mikrofrontender og deaktiverer dem.
     * Sender ut ditt nav varsel med om å deaktivere dine-pleiepenger.
     * Oppdaterer status på mikrofrontend entitet.
     */
    @Scheduled(fixedDelay = 15, timeUnit = TimeUnit.MINUTES)
    fun deaktiverAlleDinePleiepengerMicrofrontend() {
        leaderService.executeAsLeader {
            logger.info("Deaktiverer mikrofrontend for pleiepengesøknader de siste seks måneder.")
            val status = MicrofrontendAction.ENABLE
            val mikrofrontendId = MicrofrontendId.PLEIEPENGER_INNSYN.id

            val antallAktiverteDinePleiepenger = mikrofrontendRepository.countAllByMikrofrontendIdAndStatus(
                mikrofrontendId,
                status
            )
            logger.info("Fant ${antallAktiverteDinePleiepenger} aktiverte dine-pleiepenger mikrofrontend")

            mikrofrontendRepository.findAllByMikrofrontendIdAndStatus(
                mikrofrontendId,
                status
            )
                .forEach { mikrofrontendDAO: MikrofrontendDAO ->
                    runCatching {
                        sendOgLagre(mikrofrontendDAO, MicrofrontendAction.DISABLE)
                    }.onFailure {
                        logger.error("Feilet med å deaktivere dine-pleiepenger mikrofrontend. Prøver igjen senere.", it)
                    }
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
    //@Scheduled(fixedDelay = 20, timeUnit = TimeUnit.MINUTES)
    fun aktiverMikrofrontendForPleiepengesøknaderSisteSeksMåneder() = leaderService.executeAsLeader {
        logger.info("Aktiverer mikrofrontend for pleiepengesøknader de siste seks måneder.")
        søknadService.finnAlleSøknaderMedUnikeFødselsnummerForSøknadstypeSisteSeksMåneder(Søknadstype.PP_SYKT_BARN)
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
            .forEach { mikrofrontendDAO: MikrofrontendDAO ->
                runCatching {
                    sendOgLagre(mikrofrontendDAO, MicrofrontendAction.ENABLE)
                }.onFailure {
                    logger.error("Feilet med å oppdatere MikrofrontendTabell. Prøver igjen senere.", it)
                }
            }
    }

    //@Scheduled(fixedDelay = 20, timeUnit = TimeUnit.MINUTES)
    fun deaktiverMikrofrontendForPleiepengesøknaderEldreEnnSeksMåneder() = leaderService.executeAsLeader {
        logger.info("Deaktiverer mikrofrontend for pleiepengesøknader eldre enn seks måneder.")
        søknadService.finnAlleSøknaderMedUnikeFødselsnummerForSøknadstypeEldreEnnSeksMåneder(Søknadstype.PP_SYKT_BARN)
            .filter { !mikrofrontendRepository.existsByFødselsnummer(it.fødselsnummer.fødselsnummer!!) }
            .map {
                MikrofrontendDAO(
                    id = UUID.randomUUID(),
                    fødselsnummer = it.fødselsnummer.fødselsnummer!!,
                    mikrofrontendId = MicrofrontendId.PLEIEPENGER_INNSYN.id,
                    status = MicrofrontendAction.DISABLE,
                    opprettet = ZonedDateTime.now(),
                    behandlingsdato = null,
                )
            }
            .forEach { mikrofrontendDAO: MikrofrontendDAO ->
                runCatching {
                    sendOgLagre(mikrofrontendDAO, MicrofrontendAction.ENABLE)
                }.onFailure {
                    logger.error("Feilet med å oppdatere MikrofrontendTabell. Prøver igjen senere.", it)
                }
            }
    }

    @Transactional(
        transactionManager = TxConfiguration.TRANSACTION_MANAGER,
        rollbackFor = [Exception::class],
        propagation = Propagation.REQUIRES_NEW
    )
    fun sendOgLagre(mikrofrontendDAO: MikrofrontendDAO, microfrontendAction: MicrofrontendAction) {
        dittnavService.toggleMicrofrontend(mikrofrontendDAO.toK9Microfrontend(microfrontendAction))
        mikrofrontendRepository.save(mikrofrontendDAO)
    }

    private fun MikrofrontendDAO.toK9Microfrontend(microfrontendAction: MicrofrontendAction) = K9Microfrontend(
        metadata = Metadata(version = 1, correlationId = UUID.randomUUID().toString()),
        ident = fødselsnummer,
        microfrontendId = MicrofrontendId.fromId(mikrofrontendId),
        action = microfrontendAction,
        sensitivitet = Sensitivitet.SUBSTANTIAL,
        initiatedBy = "sif-innsyn-api"
    )
}

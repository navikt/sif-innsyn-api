package no.nav.sifinnsynapi.mikrofrontend

import no.nav.sifinnsynapi.common.Fødselsnummer
import no.nav.sifinnsynapi.common.Metadata
import no.nav.sifinnsynapi.common.Søknadstype
import no.nav.sifinnsynapi.config.TxConfiguration
import no.nav.sifinnsynapi.dittnav.DittnavService
import no.nav.sifinnsynapi.dittnav.K9Microfrontend
import no.nav.sifinnsynapi.dittnav.MicrofrontendAction
import no.nav.sifinnsynapi.dittnav.MicrofrontendId
import no.nav.sifinnsynapi.dittnav.Sensitivitet
import no.nav.sifinnsynapi.soknad.SøknadDAO
import no.nav.sifinnsynapi.soknad.SøknadService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class MikrofrontendService(
    private val mikrofrontendRepository: MikrofrontendRepository,
    private val søknadService: SøknadService,
    private val dittnavService: DittnavService
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(MikrofrontendService::class.java)
    }

    fun finnUnikeSøknaderUtenMikrofrontendSisteSeksMåneder(limit: Int): List<SøknadDAO> {
        return søknadService.finnUnikeSøknaderUtenMikrofrontendSisteSeksMåneder(Søknadstype.PP_SYKT_BARN, limit)
    }

    @Transactional(transactionManager = TxConfiguration.TRANSACTION_MANAGER, rollbackFor = [Exception::class])
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

    fun hentMikrofrontendIdAndStatus(
        mikrofrontendId: String,
        status: MicrofrontendAction,
        limit: Int
    ): List<MikrofrontendDAO> {
        return mikrofrontendRepository.hentMikrofrontendIdAndStatus(mikrofrontendId, status.name, limit)
    }

}

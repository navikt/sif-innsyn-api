package no.nav.sifinnsynapi.konsument.dokumentjournalforing

import kotlinx.coroutines.runBlocking
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import no.nav.sifinnsynapi.config.TxConfiguration.Companion.TRANSACTION_MANAGER
import no.nav.sifinnsynapi.saf.SafService
import no.nav.sifinnsynapi.soknad.SøknadRepository
import no.nav.sifinnsynapi.util.Constants
import no.nav.sifinnsynapi.util.MDCUtil
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class JoarkHendelseKonsument(
    private val safService: SafService,
    private val søknadRepository: SøknadRepository
) {

    companion object {
        private val logger = LoggerFactory.getLogger(JoarkHendelseKonsument::class.java)
    }

    @Transactional(TRANSACTION_MANAGER, rollbackFor = [Throwable::class])
    @KafkaListener(
        topics = ["#{'\${topic.listener.dok-journalfoering-v1.navn}'}"],
        id = "#{'\${topic.listener.dok-journalfoering-v1.id}'}",
        groupId = "#{'\${kafka.onprem.consumer.group-id}'}",
        containerFactory = "joarkKafkaJsonListenerContainerFactor",
        autoStartup = "#{'\${topic.listener.dok-journalfoering-v1.bryter}'}"
    )
    fun konsumer(
        @Payload cr: ConsumerRecord<Long, JournalfoeringHendelseRecord>
    ) {
        val hendelse = cr.value()
        logger.info("Mottatt journalføringshendelse med status: {}", hendelse.hendelsesType)

        val journalpostId = "${hendelse.journalpostId}"

        runBlocking {
            logger.info("Slår opp journalpostinfo...")
            val journalpostinfo = safService.hentJournalpostinfo(journalpostId)
            val fagsakId = journalpostinfo.sak!!.fagsakId
            MDCUtil.toMDC(Constants.K9_SAK_ID, fagsakId)
            logger.info("JournalpostInfo hentet.")

            logger.info("Oppdaterer søknad med saksId...")
            val søknad = søknadRepository.findByJournalpostId(journalpostId)
                ?: throw IllegalStateException("Søknad med journalpostId ble ikke funnet i db: $journalpostId")

            val oppdatertSøknad = søknadRepository.save(søknad.copy(saksId = fagsakId))
            logger.info("Søknad oppdatert med saksId: {}", oppdatertSøknad)

            //throw IllegalStateException("Tester transaction rollback og retrymekanisme")
        }
    }
}

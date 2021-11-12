package no.nav.sifinnsynapi.konsument.dokumentjournalforing

import kotlinx.coroutines.runBlocking
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import no.nav.sifinnsynapi.http.SøknadWithJournalpostIdNotFoundException
import no.nav.sifinnsynapi.saf.SafService
import no.nav.sifinnsynapi.soknad.SøknadService
import no.nav.sifinnsynapi.util.Constants
import no.nav.sifinnsynapi.util.MDCUtil
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service


@Service
class JoarkHendelseKonsument(
    private val safService: SafService,
    private val søknadService: SøknadService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(JoarkHendelseKonsument::class.java)
        private val K9_FAGSAK_SYSTEM = "K9"
    }

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
            logger.info("JournalpostInfo hentet.")

            val fagsak = journalpostinfo.sak

            when {
                (fagsak != null) && (!fagsak.fagsaksystem.isNullOrBlank() && fagsak.fagsaksystem == K9_FAGSAK_SYSTEM) && !fagsak.fagsakId.isNullOrBlank() -> {
                    logger.info("Fagsak: {}", fagsak)
                    MDCUtil.toMDC(Constants.K9_SAK_ID, fagsak.fagsakId)
                    logger.info("Oppdaterer søknad med saksId...")
                    try {
                        val oppdatertSøknad =
                            søknadService.oppdaterSøknadSaksIdGittJournalpostId(fagsak.fagsakId, journalpostId)
                        logger.info("Søknad oppdatert med saksId: {}", oppdatertSøknad)
                    } catch (ex: Throwable) {
                        when (ex) {
                            is SøknadWithJournalpostIdNotFoundException -> {
                                logger.info("${ex.message} Ignorerer.")
                            }
                            else -> throw ex
                        }
                    }
                }
                else -> {
                    logger.info("Sak eller fagsakId var null. Ignorerer melding.")
                }
            }
        }
    }
}

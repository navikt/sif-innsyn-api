package no.nav.sifinnsynapi.konsument.dokumentjournalforing

import kotlinx.coroutines.runBlocking
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import no.nav.sifinnsynapi.saf.SafService
import no.nav.sifinnsynapi.util.Constants
import no.nav.sifinnsynapi.util.MDCUtil
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service


@Service
class JoarkHendelseKonsument(
    private val safService: SafService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(JoarkHendelseKonsument::class.java)
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
        logger.info("Mottatt journalføringshendelse med status: {}", cr.value().hendelsesType)

        runBlocking {
            val journalpostinfo = safService.hentJournalpostinfo("${cr.value().journalpostId}")
            MDCUtil.toMDC(Constants.K9_SAK_ID, journalpostinfo.sak!!.fagsakId)
            logger.info("Hentet journalpostInfo: {}", journalpostinfo)
        }
    }
}

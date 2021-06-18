package no.nav.sifinnsynapi.konsument.dokumentjournalforing

import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import no.nav.saf.*
import no.nav.vedtak.felles.integrasjon.saf.SafJerseyTjeneste
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import java.net.URI


@Service
class JoarkHendelseKonsument {

    companion object {
        private val logger = LoggerFactory.getLogger(JoarkHendelseKonsument::class.java)

        private val JOURNALPOST_RESPONSE_PROJECTION = JournalpostResponseProjection()
            .journalpostId()
            .tittel()
            .journalposttype()
            .journalstatus()
            .datoOpprettet()
            .relevanteDatoer(
                RelevantDatoResponseProjection()
                    .dato()
                    .datotype()
            )
            .kanal()
            .tema()
            .behandlingstema()
            .sak(
                SakResponseProjection().`all$`()
            )
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
        val saf = SafJerseyTjeneste(URI("https://safselvbetjening.dev-fss-pub.nais.io"))

        val jpQuery = JournalpostQueryRequest()
        jpQuery.setJournalpostId(cr.value().journalpostId.toString())
        val journalpostInfo = saf.hentJournalpostInfo(jpQuery, JOURNALPOST_RESPONSE_PROJECTION)
        val sak = journalpostInfo.sak
        logger.info("k9-sak: {}", sak)
    }
}

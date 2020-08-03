package no.nav.sifinnsynapi.soknad

import no.nav.sifinnsynapi.common.SøknadsStatus
import no.nav.sifinnsynapi.config.Topics.INNSYN_MOTTATT
import no.nav.sifinnsynapi.util.Constants.NAV_CALL_ID
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PocHendelseKonsument(
        private val søknadRepository: SøknadRepository
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PocHendelseKonsument::class.java)
    }

    @Transactional
    @KafkaListener(topics = [INNSYN_MOTTATT], groupId = "#{'\${spring.kafka.consumer.group-id}'}", containerFactory = "kafkaJsonListenerContainerFactory")
    fun konsumer(@Payload hendelse: SøknadsHendelse, @Header(name = NAV_CALL_ID, required = false) callId: String?) {
        logger.info("Mottok hendelse {}", hendelse)

        val hendelseSomSøknadDAO = hendelse.tilSøknadDAO()

        when(hendelse.status){
            SøknadsStatus.MOTTATT -> søknadRepository.save(hendelseSomSøknadDAO)
            SøknadsStatus.UNDER_BEHANDLING -> {
                val søknad = søknadRepository.findByJournalpostId(hendelse.journalpostId)
                if (søknad != null) {
                    val oppdatertSøknad = søknad.copy(status = hendelse.status)
                    søknadRepository.save(oppdatertSøknad)
                }
            }
            SøknadsStatus.FERDIG_BEHANDLET -> {
                val søknad = søknadRepository.findByJournalpostId(hendelse.journalpostId)
                if (søknad != null) {
                    val oppdatertSøknad = søknad.copy(status = hendelse.status)
                    søknadRepository.save(oppdatertSøknad)
                }
            }
        }
    }
}

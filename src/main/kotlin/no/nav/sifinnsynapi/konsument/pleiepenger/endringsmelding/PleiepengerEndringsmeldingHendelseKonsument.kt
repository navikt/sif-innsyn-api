package no.nav.sifinnsynapi.konsument.pleiepenger.endringsmelding

import no.nav.sifinnsynapi.common.TopicEntry
import no.nav.sifinnsynapi.config.TxConfiguration
import no.nav.sifinnsynapi.soknad.SøknadRepository
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class PleiepengerEndringsmeldingHendelseKonsument(
    private val repository: SøknadRepository
) {

    private val logger = LoggerFactory.getLogger(PleiepengerEndringsmeldingHendelseKonsument::class.java)

    @Transactional(TxConfiguration.TRANSACTION_MANAGER)
    @KafkaListener(
        topics = ["#{'\${topic.listener.pp-endringsmelding.navn}'}"],
        id = "#{'\${topic.listener.pp-endringsmelding.id}'}",
        groupId = "#{'\${kafka.aiven.consumer.group-id}'}",
        containerFactory = "aivenKafkaJsonListenerContainerFactory",
        autoStartup = "#{'\${topic.listener.pp-endringsmelding.bryter}'}"
    )
    fun konsumer(
        @Payload hendelse: TopicEntry
    ){
        logger.info("KONSUMERER PP ENDRINGSMELDING. $hendelse")
        val melding = JSONObject(hendelse.data.melding)

    }
}
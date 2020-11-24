package no.nav.sifinnsynapi.omsorgspenger.midlertidigalene

import no.nav.sifinnsynapi.common.TopicEntry
import no.nav.sifinnsynapi.dittnav.DittnavService
import no.nav.sifinnsynapi.dittnav.PleiepengerDittnavBeskjedProperties
import no.nav.sifinnsynapi.omsorgspenger.midlertidigalene.OmsorgspengerMidlertidigAleneKonsument.Companion.Keys.SØKNAD_ID
import no.nav.sifinnsynapi.pleiepenger.syktbarn.somK9Beskjed
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class OmsorgspengerMidlertidigAleneKonsument(
        private val dittNavService: DittnavService,
        private val pleiepengerDittnavBeskjedProperties: PleiepengerDittnavBeskjedProperties, //Må lage egen for oms. Kanskje refaktoreres til en felles klasse og subklasser. Uten link
        ){

    companion object {
        private val logger = LoggerFactory.getLogger(OmsorgspengerMidlertidigAleneKonsument::class.java)
        private val YTELSE = "'omsorgspenger - midlertidig alene'"

        internal object Keys {
            const val SØKNAD_ID = "søknadId"
            const val SØKER = "søker"
            const val AKTØR_ID = "aktørId"
            const val MOTTATT = "mottatt"
            const val FØDSELSNUMMER = "fødselsnummer"
        }
    }

    @Transactional
    @KafkaListener(
            topics = ["#{'\${topic.listener.omp-midlertidig-alene.navn}'}"],
            id = "#{'\${topic.listener.omp-midlertidig-alene.id}'}",
            groupId = "#{'\${spring.kafka.consumer.group-id}'}",
            containerFactory = "kafkaJsonListenerContainerFactory",
            autoStartup = "#{'\${topic.listener.omp-midlertidig-alene.bryter}'}"
    )
    fun konsumer(
            @Payload hendelse: TopicEntry
    ){
        val melding = JSONObject(hendelse.data.melding)
        val søknadId = melding.getString(SØKNAD_ID)
        logger.info("Mottok hendelse om ${YTELSE} med søknadId: $søknadId")

        val dittNavBeskjed = melding.somK9Beskjed(hendelse.data.metadata, pleiepengerDittnavBeskjedProperties)

        dittNavService.sendBeskjed(
                melding.getString(SØKNAD_ID),
                dittNavBeskjed
        )
    }

}
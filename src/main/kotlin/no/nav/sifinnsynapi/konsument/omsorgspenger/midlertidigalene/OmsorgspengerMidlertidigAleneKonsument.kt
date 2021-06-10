package no.nav.sifinnsynapi.konsument.omsorgspenger.midlertidigalene

import no.nav.sifinnsynapi.common.Metadata
import no.nav.sifinnsynapi.common.TopicEntry
import no.nav.sifinnsynapi.dittnav.DittnavService
import no.nav.sifinnsynapi.dittnav.K9Beskjed
import no.nav.sifinnsynapi.konsument.omsorgspenger.midlertidigalene.OmsorgspengerMidlertidigAleneKonsument.Companion.Keys.FØDSELSNUMMER
import no.nav.sifinnsynapi.konsument.omsorgspenger.midlertidigalene.OmsorgspengerMidlertidigAleneKonsument.Companion.Keys.SØKER
import no.nav.sifinnsynapi.konsument.omsorgspenger.midlertidigalene.OmsorgspengerMidlertidigAleneKonsument.Companion.Keys.SØKNAD_ID
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import java.util.*

@Service
class OmsorgspengerMidlertidigAleneKonsument(
        private val dittNavService: DittnavService,
        private val omsorgspengerMidlertidigAleneBeskjedProperties: OmsorgspengerMidlertidigAleneBeskjedProperties
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

    @KafkaListener(
            topics = ["#{'\${topic.listener.omp-midlertidig-alene.navn}'}"],
            id = "#{'\${topic.listener.omp-midlertidig-alene.id}'}",
            groupId = "#{'\${kafka.onprem.consumer.group-id}'}",
            containerFactory = "onpremKafkaJsonListenerContainerFactory",
            autoStartup = "#{'\${topic.listener.omp-midlertidig-alene.bryter}'}"
    )
    fun konsumer(
            @Payload hendelse: TopicEntry
    ){
        val melding = JSONObject(hendelse.data.melding)
        val søknadId = melding.getString(SØKNAD_ID)
        logger.info("Mottok hendelse om ${YTELSE} med søknadId: $søknadId")

        logger.info("Sender DittNav beskjed for ytelse $YTELSE")
        dittNavService.sendBeskjedOnprem(
                melding.getString(SØKNAD_ID),
                melding.somK9Beskjed(hendelse.data.metadata, omsorgspengerMidlertidigAleneBeskjedProperties)
        )
    }
}

private fun JSONObject.somK9Beskjed(metadata: Metadata, beskjedProperties: OmsorgspengerMidlertidigAleneBeskjedProperties): K9Beskjed {
    val søknadId = getString(SØKNAD_ID)
    return K9Beskjed(
            metadata = metadata,
            søkerFødselsnummer = getJSONObject(SØKER).getString(FØDSELSNUMMER),
            tekst = beskjedProperties.tekst,
            link = beskjedProperties.link,
            grupperingsId = søknadId,
            eventId = UUID.randomUUID().toString(),
            dagerSynlig = beskjedProperties.dagerSynlig
    )
}

package no.nav.sifinnsynapi.konsument.omsorgsdager.aleneomsorg

import no.nav.sifinnsynapi.common.Metadata
import no.nav.sifinnsynapi.common.TopicEntry
import no.nav.sifinnsynapi.dittnav.DittnavService
import no.nav.sifinnsynapi.dittnav.K9Beskjed
import no.nav.sifinnsynapi.konsument.omsorgsdager.aleneomsorg.OmsorgspengerAleneomsorgKonsument.Companion.Keys.FØDSELSNUMMER
import no.nav.sifinnsynapi.konsument.omsorgsdager.aleneomsorg.OmsorgspengerAleneomsorgKonsument.Companion.Keys.SØKER
import no.nav.sifinnsynapi.konsument.omsorgsdager.aleneomsorg.OmsorgspengerAleneomsorgKonsument.Companion.Keys.SØKNAD_ID
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import java.util.*

@Service
class OmsorgspengerAleneomsorgKonsument(
        private val dittNavService: DittnavService,
        private val omsorgsdagerAleneomsorgBeskjedProperties: OmsorgsdagerAleneomsorgBeskjedProperties
    ){

    companion object {
        private val logger = LoggerFactory.getLogger(OmsorgspengerAleneomsorgKonsument::class.java)
        private val YTELSE = "'omsorgsdager - aleneomsorg'"

        internal object Keys {
            const val SØKNAD_ID = "søknadId"
            const val SØKER = "søker"
            const val AKTØR_ID = "aktørId"
            const val MOTTATT = "mottatt"
            const val FØDSELSNUMMER = "fødselsnummer"
        }
    }

    @KafkaListener(
        topics = ["#{'\${topic.listener.omd-aleneomsorg.navn}'}"],
        id = "#{'\${topic.listener.omd-aleneomsorg.id}'}",
        autoStartup = "#{'\${topic.listener.omd-aleneomsorg.bryter}'}",
        groupId = "#{'\${kafka.aiven.consumer.group-id}'}",
        containerFactory = "aivenKafkaJsonListenerContainerFactory",
    )
    fun konsumer(
        @Payload hendelse: TopicEntry
    ){
        val melding = JSONObject(hendelse.data.melding)
        val søknadId = melding.getString(SØKNAD_ID)
        logger.info("Mottok hendelse om $YTELSE med søknadId: $søknadId")

        logger.info("Sender DittNav beskjed for ytelse $YTELSE")
        dittNavService.sendBeskjedAiven(
            melding.getString(SØKNAD_ID),
            melding.somK9Beskjed(hendelse.data.metadata, omsorgsdagerAleneomsorgBeskjedProperties)
        )
    }
}

private fun JSONObject.somK9Beskjed(metadata: Metadata, beskjedProperties: OmsorgsdagerAleneomsorgBeskjedProperties): K9Beskjed {
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

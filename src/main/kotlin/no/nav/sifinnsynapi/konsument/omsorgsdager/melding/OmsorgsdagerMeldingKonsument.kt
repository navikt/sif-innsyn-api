package no.nav.sifinnsynapi.konsument.omsorgsdager.melding

import no.nav.sifinnsynapi.common.Metadata
import no.nav.sifinnsynapi.common.TopicEntry
import no.nav.sifinnsynapi.dittnav.DittnavService
import no.nav.sifinnsynapi.dittnav.K9Beskjed
import no.nav.sifinnsynapi.dittnav.K9BeskjedProperties
import no.nav.sifinnsynapi.konsument.omsorgsdager.OmsorgsdagerMeldingFordeleBeskjedProperties
import no.nav.sifinnsynapi.konsument.omsorgsdager.OmsorgsdagerMeldingKoronaBeskjedProperties
import no.nav.sifinnsynapi.konsument.omsorgsdager.OmsorgsdagerMeldingOverforeBeskjedProperties
import no.nav.sifinnsynapi.konsument.omsorgsdager.melding.OmsorgsdagerMeldingKonsument.Companion.Keys.FØDSELSNUMMER
import no.nav.sifinnsynapi.konsument.omsorgsdager.melding.OmsorgsdagerMeldingKonsument.Companion.Keys.MELDING_TYPE
import no.nav.sifinnsynapi.konsument.omsorgsdager.melding.OmsorgsdagerMeldingKonsument.Companion.Keys.SØKER
import no.nav.sifinnsynapi.konsument.omsorgsdager.melding.OmsorgsdagerMeldingKonsument.Companion.Keys.SØKNAD_ID
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import java.util.*

@Service
class OmsorgsdagerMeldingKonsument(
    private val dittNavService: DittnavService,
    private val omsorgsdagerMeldingKoronaBeskjedProperties: OmsorgsdagerMeldingKoronaBeskjedProperties,
    private val omsorgsdagerMeldingOverforeBeskjedProperties: OmsorgsdagerMeldingOverforeBeskjedProperties,
    private val omsorgsdagerMeldingFordeleBeskjedProperties: OmsorgsdagerMeldingFordeleBeskjedProperties
){

    companion object {
        private val logger = LoggerFactory.getLogger(OmsorgsdagerMeldingKonsument::class.java)
        private val YTELSE = "omsorgsdager"

        internal object Keys {
            const val SØKNAD_ID = "søknadId"
            const val SØKER = "søker"
            const val FØDSELSNUMMER = "fødselsnummer"
            const val MELDING_TYPE = "type"
        }

        enum class Meldingstype(val utskriftsvennlig: String) {
            KORONA("koronaoverføring"),
            OVERFORING("overføring"),
            FORDELING("fordeling")
        }
    }

    @KafkaListener(
            topics = ["#{'\${topic.listener.omd-melding.navn}'}"],
            id = "#{'\${topic.listener.omd-melding.id}'}",
            groupId = "#{'\${kafka.onprem.consumer.group-id}'}",
            containerFactory = "onpremKafkaJsonListenerContainerFactory",
            autoStartup = "#{'\${topic.listener.omd-melding.bryter}'}"
    )
    fun konsumer(
            @Payload hendelse: TopicEntry
    ){
        val melding = JSONObject(hendelse.data.melding)
        val søknadId = melding.getString(SØKNAD_ID)
        val meldingstype = Meldingstype.valueOf(melding.getString(MELDING_TYPE))

        logger.info("Mottok hendelse om '$YTELSE - ${meldingstype.utskriftsvennlig}' med søknadId: $søknadId")

        val beskjedProperties = when(meldingstype){
            Meldingstype.KORONA -> omsorgsdagerMeldingKoronaBeskjedProperties
            Meldingstype.OVERFORING -> omsorgsdagerMeldingOverforeBeskjedProperties
            Meldingstype.FORDELING -> omsorgsdagerMeldingFordeleBeskjedProperties
        }

        logger.info("Sender DittNav beskjed for ytelse $YTELSE - $meldingstype")
        dittNavService.sendBeskjedOnprem(
                søknadId = melding.getString(SØKNAD_ID),
                k9Beskjed = melding.somK9Beskjed(
                        metadata = hendelse.data.metadata,
                        beskjedProperties = beskjedProperties
                )
        )
    }
}

private fun JSONObject.somK9Beskjed(metadata: Metadata, beskjedProperties: K9BeskjedProperties): K9Beskjed {
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

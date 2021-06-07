package no.nav.sifinnsynapi.konsumenter.k9ettersending

import no.nav.sifinnsynapi.common.Metadata
import no.nav.sifinnsynapi.common.TopicEntry
import no.nav.sifinnsynapi.dittnav.DittnavService
import no.nav.sifinnsynapi.dittnav.K9Beskjed
import no.nav.sifinnsynapi.dittnav.K9BeskjedProperties
import no.nav.sifinnsynapi.konsumenter.k9ettersending.K9EttersendingKonsument.Companion.Keys.FØDSELSNUMMER
import no.nav.sifinnsynapi.konsumenter.k9ettersending.K9EttersendingKonsument.Companion.Keys.SØKER
import no.nav.sifinnsynapi.konsumenter.k9ettersending.K9EttersendingKonsument.Companion.Keys.SØKNAD_ID
import no.nav.sifinnsynapi.konsumenter.k9ettersending.K9EttersendingKonsument.Companion.Keys.SØKNAD_TYPE
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class K9EttersendingKonsument(
        private val dittNavService: DittnavService,
        private val k9EttersendingPPBeskjedProperties: K9EttersendingPPBeskjedProperties,
        private val k9EttersendingOMSBeskjedProperties: K9EttersendingOMSBeskjedProperties,
    ){

    companion object {
        private val logger = LoggerFactory.getLogger(K9EttersendingKonsument::class.java)
        private val YTELSE = "ettersending"

        internal object Keys {
            const val SØKNAD_ID = "soknadId"
            const val SØKER = "søker"
            const val FØDSELSNUMMER = "fødselsnummer"
            const val SØKNAD_TYPE = "søknadstype"
        }

        enum class Søknadstype(val utskriftsvennlig: String) {
            PLEIEPENGER("pleiepenger"), //TODO 24.03.2021 - Kan fjernes når k9-ettersending-prosessering er prodsatt
            OMSORGSPENGER("omsorgspenger"), //TODO 24.03.2021 - Kan fjernes når k9-ettersending-prosessering er prodsatt
            PLEIEPENGER_SYKT_BARN("pleiepenger"),
            OMP_UTV_KS("omsorgspenger"), // Omsorgspenger utvidet rett - kronisk syke eller funksjonshemming.
            OMP_UT_SNF("omsorgspenger"), // Omsorgspenger utbetaling SNF ytelse.
            OMP_UT_ARBEIDSTAKER("omsorgspenger"), // Omsorgspenger utbetaling arbeidstaker ytelse.
            OMP_UTV_MA("omsorgspenger"), // Omsorgspenger utvidet rett - midlertidig alene
            OMP_DELE_DAGER("omsorgspenger")
        }
    }

    @KafkaListener(
            topics = ["#{'\${topic.listener.k9-ettersending.navn}'}"],
            id = "#{'\${topic.listener.k9-ettersending.id}'}",
            groupId = "#{'\${kafka.onprem.consumer.group-id}'}",
            containerFactory = "onpremKafkaJsonListenerContainerFactory",
            autoStartup = "#{'\${topic.listener.k9-ettersending.bryter}'}"
    )
    fun konsumer(
            @Payload hendelse: TopicEntry
    ){
        val melding = JSONObject(hendelse.data.melding)
        val søknadId = melding.getString(SØKNAD_ID)
        val søknadstype = Søknadstype.valueOf(melding.getString(SØKNAD_TYPE).toUpperCase())

        logger.info("Mottok hendelse om '$YTELSE - ${søknadstype.utskriftsvennlig}' med søknadId: $søknadId")

        val beskjedProperties = when(søknadstype){
            Søknadstype.PLEIEPENGER, Søknadstype.PLEIEPENGER_SYKT_BARN -> k9EttersendingPPBeskjedProperties
            else -> k9EttersendingOMSBeskjedProperties
        }

        logger.info("Sender DittNav beskjed for ytelse $YTELSE - ${søknadstype.utskriftsvennlig}")
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

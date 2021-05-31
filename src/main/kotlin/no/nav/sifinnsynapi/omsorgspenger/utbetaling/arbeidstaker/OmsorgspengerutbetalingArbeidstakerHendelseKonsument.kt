package no.nav.sifinnsynapi.omsorgspenger.utbetaling.arbeidstaker

import no.nav.sifinnsynapi.common.Metadata
import no.nav.sifinnsynapi.common.TopicEntry
import no.nav.sifinnsynapi.dittnav.DittnavService
import no.nav.sifinnsynapi.dittnav.K9Beskjed
import no.nav.sifinnsynapi.omsorgspenger.utbetaling.arbeidstaker.OmsorgspengerutbetalingArbeidstakerHendelseKonsument.Companion.Keys.FØDSELSNUMMER
import no.nav.sifinnsynapi.omsorgspenger.utbetaling.arbeidstaker.OmsorgspengerutbetalingArbeidstakerHendelseKonsument.Companion.Keys.SØKER
import no.nav.sifinnsynapi.omsorgspenger.utbetaling.arbeidstaker.OmsorgspengerutbetalingArbeidstakerHendelseKonsument.Companion.Keys.SØKNAD_ID
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class OmsorgspengerutbetalingArbeidstakerHendelseKonsument(
        private val dittNavService: DittnavService,
        private val omsorgspengerutbetalingArbeidstakerBeskjedProperties: OmsorgspengerutbetalingArbeidstakerBeskjedProperties
    ){

    companion object {
        private val logger = LoggerFactory.getLogger(OmsorgspengerutbetalingArbeidstakerHendelseKonsument::class.java)
        private val YTELSE = "'omsorgspengerutbetaling - arbeidstaker'"

        internal object Keys {
            const val SØKNAD_ID = "soknadId"
            const val SØKER = "søker"
            const val AKTØR_ID = "aktørId"
            const val MOTTATT = "mottatt"
            const val FØDSELSNUMMER = "fødselsnummer"
        }
    }

    @Transactional
    @KafkaListener(
            topics = ["#{'\${topic.listener.omp-utbetaling-arbeidstaker.navn}'}"],
            id = "#{'\${topic.listener.omp-utbetaling-arbeidstaker.id}'}",
            groupId = "#{'\${kafka.onprem.consumer.group-id}'}",
            containerFactory = "onpremKafkaJsonListenerContainerFactory",
            autoStartup = "#{'\${topic.listener.omp-utbetaling-arbeidstaker.bryter}'}"
    )
    fun konsumer(
            @Payload hendelse: TopicEntry
    ){
        val melding = JSONObject(hendelse.data.melding)
        val søknadId = melding.getString(SØKNAD_ID)
        logger.info("Mottok hendelse om $YTELSE med søknadId: $søknadId")

        logger.info("Sender DittNav beskjed for ytelse $YTELSE")
        dittNavService.sendBeskjed(
                melding.getString(SØKNAD_ID),
                melding.somK9Beskjed(hendelse.data.metadata, omsorgspengerutbetalingArbeidstakerBeskjedProperties)
        )
    }
}

private fun JSONObject.somK9Beskjed(metadata: Metadata, beskjedProperties: OmsorgspengerutbetalingArbeidstakerBeskjedProperties): K9Beskjed {
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

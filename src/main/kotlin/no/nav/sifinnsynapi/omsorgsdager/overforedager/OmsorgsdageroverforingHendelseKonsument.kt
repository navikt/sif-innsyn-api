package no.nav.sifinnsynapi.omsorgsdager.overforedager

import no.nav.sifinnsynapi.common.*
import no.nav.sifinnsynapi.dittnav.PleiepengerDittnavBeskjedProperties
import no.nav.sifinnsynapi.pleiepenger.syktbarn.PleiepengerSyktBarnHendelseKonsument.Companion.Keys.AKTØR_ID
import no.nav.sifinnsynapi.pleiepenger.syktbarn.PleiepengerSyktBarnHendelseKonsument.Companion.Keys.FØDSELSNUMMER
import no.nav.sifinnsynapi.pleiepenger.syktbarn.PleiepengerSyktBarnHendelseKonsument.Companion.Keys.MOTTATT
import no.nav.sifinnsynapi.pleiepenger.syktbarn.PleiepengerSyktBarnHendelseKonsument.Companion.Keys.SØKER
import no.nav.sifinnsynapi.pleiepenger.syktbarn.PleiepengerSyktBarnHendelseKonsument.Companion.Keys.SØKNAD_ID
import no.nav.sifinnsynapi.soknad.Søknad
import no.nav.sifinnsynapi.soknad.SøknadDAO
import no.nav.sifinnsynapi.soknad.SøknadRepository
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.*

@Service
class OmsorgsdagerOverforingHendelseKonsument(
        private val repository: SøknadRepository,
        @Value("\${topic.listener.omd-overforing.dry-run}") private val dryRun: Boolean
) {
    companion object {
        private val logger = LoggerFactory.getLogger(OmsorgsdagerOverforingHendelseKonsument::class.java)
        private val YTELSE = "'omsorgsdager - overføring'"

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
            topics = ["#{'\${topic.listener.omd-overforing.navn}'}"],
            id = "#{'\${topic.listener.omd-overforing.id}'}",
            groupId = "#{'\${spring.kafka.consumer.group-id}'}",
            containerFactory = "kafkaK9RapidJsonListenerContainerFactory",
            autoStartup = "#{'\${topic.listener.omd-overforing.bryter}'}"
    )
    fun konsumer(
            @Payload hendelse: Object // TODO: Oppdatere med riktig type
    ) {
        logger.info("Konsumerer hendelse om $YTELSE")
    }
}


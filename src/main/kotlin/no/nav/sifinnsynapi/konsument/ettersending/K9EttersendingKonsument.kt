package no.nav.sifinnsynapi.konsument.ettersending

import no.nav.sifinnsynapi.common.*
import no.nav.sifinnsynapi.config.TxConfiguration
import no.nav.sifinnsynapi.dittnav.DittnavService
import no.nav.sifinnsynapi.dittnav.byggK9Beskjed
import no.nav.sifinnsynapi.konsument.ettersending.K9EttersendingKonsument.Companion.Keys.AKTØR_ID
import no.nav.sifinnsynapi.konsument.ettersending.K9EttersendingKonsument.Companion.Keys.FØDSELSNUMMER
import no.nav.sifinnsynapi.konsument.ettersending.K9EttersendingKonsument.Companion.Keys.MOTTATT
import no.nav.sifinnsynapi.konsument.ettersending.K9EttersendingKonsument.Companion.Keys.SØKER
import no.nav.sifinnsynapi.konsument.ettersending.K9EttersendingKonsument.Companion.Keys.SØKNAD_ID
import no.nav.sifinnsynapi.konsument.ettersending.K9EttersendingKonsument.Companion.Keys.SØKNAD_TYPE
import no.nav.sifinnsynapi.konsument.ettersending.K9EttersendingKonsument.Companion.Søknadstype.*
import no.nav.sifinnsynapi.soknad.Søknad
import no.nav.sifinnsynapi.soknad.SøknadRepository
import no.nav.sifinnsynapi.util.storForbokstav
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Service
class K9EttersendingKonsument(
    private val søknadRepository: SøknadRepository,
    private val dittNavService: DittnavService,
    private val k9EttersendingPPBeskjedProperties: K9EttersendingPPBeskjedProperties,
    private val k9EttersendingOMSBeskjedProperties: K9EttersendingOMSBeskjedProperties,
    @Value("\${topic.listener.k9-ettersending.dry-run}") private val dryRun: Boolean
) {

    companion object {
        private val logger = LoggerFactory.getLogger(K9EttersendingKonsument::class.java)
        private val YTELSE = "ettersending"

        internal object Keys {
            const val MOTTATT = "mottatt"
            const val SØKNAD_ID = "soknadId"
            const val SØKER = "søker"
            const val AKTØR_ID = "aktørId"
            const val FØDSELSNUMMER = "fødselsnummer"
            const val SØKNAD_TYPE = "søknadstype"
        }

        enum class Søknadstype(val utskriftsvennlig: String) {
            PLEIEPENGER_SYKT_BARN("pleiepenger"),
            PLEIEPENGER_LIVETS_SLUTTFASE("pleiepenger livets sluttfase"),
            OMP_UTV_KS("omsorgspenger"), // Omsorgspenger utvidet rett - kronisk syke eller funksjonshemming.
            OMP_UT_SNF("omsorgspenger"), // Omsorgspenger utbetaling SNF ytelse.
            OMP_UT_ARBEIDSTAKER("omsorgspenger"), // Omsorgspenger utbetaling arbeidstaker ytelse.
            OMP_DELE_DAGER("omsorgspenger"),
            OMP_UTV_MA("omsorgspenger"), // Omsorgspenger utvidet rett - midlertidig alene
        }
    }

    @Transactional(TxConfiguration.TRANSACTION_MANAGER)
    @KafkaListener(
        topics = ["#{'\${topic.listener.k9-ettersending.navn}'}"],
        id = "#{'\${topic.listener.k9-ettersending.id}'}",
        groupId = "#{'\${kafka.aiven.consumer.group-id}'}",
        containerFactory = "aivenKafkaJsonListenerContainerFactory",
        autoStartup = "#{'\${topic.listener.k9-ettersending.bryter}'}"
    )
    fun konsumer(
        @Payload hendelse: TopicEntry
    ) {
        if (dryRun) {
            logger.info("DRY RUN - ettersendelse om $YTELSE")
        } else {
            val melding = JSONObject(hendelse.data.melding)
            val søknadId = melding.getString(SØKNAD_ID)
            val søknadstype = valueOf(melding.getString(SØKNAD_TYPE).storForbokstav())

            logger.info("Mottok hendelse om '$YTELSE - ${søknadstype.utskriftsvennlig}' med søknadId: $søknadId")

            val beskjedProperties = when (søknadstype) {
                PLEIEPENGER_SYKT_BARN, PLEIEPENGER_LIVETS_SLUTTFASE -> k9EttersendingPPBeskjedProperties
                else -> k9EttersendingOMSBeskjedProperties
            }

            val søknadsHendelse = Søknad(
                aktørId = AktørId(melding.getJSONObject(SØKER).getString(AKTØR_ID)),
                mottattDato = ZonedDateTime.parse(melding.getString(MOTTATT)),
                fødselsnummer = Fødselsnummer(melding.getJSONObject(SØKER).getString(FØDSELSNUMMER)),
                journalpostId = hendelse.data.journalførtMelding.journalpostId,
                søknadstype = when (søknadstype) {
                    PLEIEPENGER_SYKT_BARN -> no.nav.sifinnsynapi.common.Søknadstype.PP_ETTERSENDELSE
                    PLEIEPENGER_LIVETS_SLUTTFASE -> no.nav.sifinnsynapi.common.Søknadstype.PP_LIVETS_SLUTTFASE_ETTERSENDELSE
                    else -> no.nav.sifinnsynapi.common.Søknadstype.OMS_ETTERSENDELSE
                },
                status = SøknadsStatus.MOTTATT,
                søknad = hendelse.data.melding
            )

            logger.info("Lagrer melding om ettersending for $søknadstype")
            val ettersending = søknadRepository.save(søknadsHendelse.tilSøknadDAO(søknadId))
            logger.info("Ettersendelse for $søknadstype lagret: {}", ettersending)

            logger.info("Sender DittNav beskjed for ytelse $YTELSE - ${søknadstype.utskriftsvennlig}")
            val k9Beskjed = byggK9Beskjed(hendelse.data.metadata, søknadId, beskjedProperties, melding.getJSONObject(SØKER).getString(FØDSELSNUMMER) )
            dittNavService.sendBeskjedAiven(k9Beskjed = k9Beskjed)
        }
    }
}
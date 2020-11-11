package no.nav.sifinnsynapi.omsorgspenger.utbetaling.snf

import no.nav.sifinnsynapi.common.*
import no.nav.sifinnsynapi.dokument.DokumentRepository
import no.nav.sifinnsynapi.omsorgspenger.utbetaling.snf.OmsorgspengerutbetalingSNFHendelseKonsument.Companion.Keys.AKTØR_ID
import no.nav.sifinnsynapi.omsorgspenger.utbetaling.snf.OmsorgspengerutbetalingSNFHendelseKonsument.Companion.Keys.FØDSELSNUMMER
import no.nav.sifinnsynapi.omsorgspenger.utbetaling.snf.OmsorgspengerutbetalingSNFHendelseKonsument.Companion.Keys.MOTTATT
import no.nav.sifinnsynapi.omsorgspenger.utbetaling.snf.OmsorgspengerutbetalingSNFHendelseKonsument.Companion.Keys.SØKER
import no.nav.sifinnsynapi.omsorgspenger.utbetaling.snf.OmsorgspengerutbetalingSNFHendelseKonsument.Companion.Keys.SØKNAD_ID
import no.nav.sifinnsynapi.soknad.Søknad
import no.nav.sifinnsynapi.soknad.SøknadDAO
import no.nav.sifinnsynapi.soknad.SøknadRepository
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.*


@Service
class OmsorgspengerutbetalingSNFHendelseKonsument(
        private val søknadRepo: SøknadRepository,
        private val dokumentRepo: DokumentRepository
) {
    companion object {
        private val logger = LoggerFactory.getLogger(OmsorgspengerutbetalingSNFHendelseKonsument::class.java)
        private val YTELSE = "'omsorgspengerutbetaling - snf'"

        private object Keys {
            const val SØKNAD_ID = "søknadId"
            const val SØKER = "søker"
            const val AKTØR_ID = "aktørId"
            const val MOTTATT = "mottatt"
            const val FØDSELSNUMMER = "fødselsnummer"
        }
    }

    @Transactional
    @KafkaListener(
            topics = ["#{'\${topic.listener.omp-utbetaling-snf.navn}'}"],
            id = "#{'\${topic.listener.omp-utbetaling-snf.id}'}",
            groupId = "#{'\${spring.kafka.consumer.group-id}'}",
            containerFactory = "kafkaJsonListenerContainerFactory",
            autoStartup = "#{'\${topic.listener.omp-utbetaling-snf.bryter}'}"
    )
    fun konsumer(
            @Payload hendelse: TopicEntry
    ) {
        val melding = JSONObject(hendelse.data.melding)
        val søknadId = melding.getString(SØKNAD_ID)
        logger.info("Mottok hendelse om $YTELSE med søknadId: $søknadId")

        logger.info("Mapper om fra TopicEntry til Søknad for $YTELSE")
        val søknadsHendelse = Søknad(
                aktørId = AktørId(melding.getJSONObject(SØKER).getString(AKTØR_ID)),
                mottattDato = ZonedDateTime.parse(melding.getString(MOTTATT)),
                fødselsnummer = Fødselsnummer(melding.getJSONObject(SØKER).getString(FØDSELSNUMMER)),
                journalpostId = hendelse.data.journalførtMelding.journalpostId,
                søknadstype = Søknadstype.OMP_UTBETALING_SNF,
                status = SøknadsStatus.MOTTATT,
                søknad = hendelse.data.melding
        )

        logger.info("Lagrer Søknad for $YTELSE")
        val søknadDAO = søknadRepo.save(søknadsHendelse.tilSøknadDAO())
        logger.info("Søknad for $YTELSE lagret: {}", søknadDAO)
    }

    private fun Søknad.tilSøknadDAO(): SøknadDAO = SøknadDAO(
            id = UUID.fromString(JSONObject(søknad).getString(SØKNAD_ID)),
            aktørId = aktørId,
            saksId = saksnummer,
            fødselsnummer = fødselsnummer,
            journalpostId = journalpostId,
            søknad = JSONObject(søknad).toString(),
            status = status,
            søknadstype = søknadstype,
            behandlingsdato = førsteBehandlingsdato,
            opprettet = mottattDato,
            endret = null
    )
}

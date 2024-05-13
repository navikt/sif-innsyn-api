package no.nav.sifinnsynapi.konsument.pleiepenger.syktbarn

import no.nav.sifinnsynapi.common.AktørId
import no.nav.sifinnsynapi.common.Fødselsnummer
import no.nav.sifinnsynapi.common.SøknadsStatus
import no.nav.sifinnsynapi.common.Søknadstype
import no.nav.sifinnsynapi.common.TopicEntry
import no.nav.sifinnsynapi.config.TxConfiguration.Companion.TRANSACTION_MANAGER
import no.nav.sifinnsynapi.dittnav.DittnavService
import no.nav.sifinnsynapi.dittnav.byggK9Beskjed
import no.nav.sifinnsynapi.konsument.pleiepenger.syktbarn.PleiepengersøknadKeysV1.AKTØR_ID
import no.nav.sifinnsynapi.konsument.pleiepenger.syktbarn.PleiepengersøknadKeysV1.FØDSELSNUMMER
import no.nav.sifinnsynapi.konsument.pleiepenger.syktbarn.PleiepengersøknadKeysV1.MOTTATT
import no.nav.sifinnsynapi.konsument.pleiepenger.syktbarn.PleiepengersøknadKeysV1.SØKER
import no.nav.sifinnsynapi.konsument.pleiepenger.syktbarn.PleiepengersøknadKeysV1.SØKNAD_ID
import no.nav.sifinnsynapi.soknad.Søknad
import no.nav.sifinnsynapi.soknad.SøknadRepository
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Service
class PleiepengerSyktBarnHendelseKonsument(
        private val repository: SøknadRepository,
        private val dittnavService: DittnavService,
        private val pleiepengerDittnavBeskjedProperties: PleiepengerDittnavBeskjedProperties,
        @Value("\${topic.listener.pp-sykt-barn.dry-run}") private val dryRun: Boolean
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PleiepengerSyktBarnHendelseKonsument::class.java)
        private val YTELSE = "'pleiepenger - sykt barn'"
    }

    @Transactional(transactionManager = TRANSACTION_MANAGER, rollbackFor = [Exception::class])
    @KafkaListener(
            topics = ["#{'\${topic.listener.pp-sykt-barn.navn}'}"],
            id = "#{'\${topic.listener.pp-sykt-barn.id}'}",
            groupId = "#{'\${kafka.aiven.consumer.group-id}'}",
            containerFactory = "aivenKafkaJsonListenerContainerFactory",
            autoStartup = "#{'\${topic.listener.pp-sykt-barn.bryter}'}"
    )
    fun konsumer(
            @Payload hendelse: TopicEntry
    ) {
        val melding = JSONObject(hendelse.data.melding)
        val søknadId = melding.getString(SØKNAD_ID)

        if (dryRun) {
            logger.info("DRY_RUN --> Mottok hendelse om $YTELSE med søknadId: $søknadId")
            logger.info("DRY_RUN --> Mapper fra TopicEntry til Søknad for $YTELSE")
            try {
                Søknad(
                        aktørId = AktørId(melding.getJSONObject(SØKER).getString(AKTØR_ID)),
                        mottattDato = ZonedDateTime.parse(melding.getString(MOTTATT)),
                        fødselsnummer = Fødselsnummer(melding.getJSONObject(SØKER).getString(FØDSELSNUMMER)),
                        journalpostId = hendelse.data.journalførtMelding.journalpostId,
                        søknadstype = Søknadstype.PP_SYKT_BARN,
                        status = SøknadsStatus.MOTTATT,
                        søknad = hendelse.data.melding
                )
            } catch (ex: Exception) {
                logger.error("DRY_RUN --> Feilet med å mappe om TopicEntry til Søknad for $YTELSE")
            }
        } else {
            logger.info("Mottok hendelse om $YTELSE med søknadId: $søknadId\"")

            logger.info("Mapper om fra TopicEntry til Søknad for $YTELSE")

            val søknadsHendelse = Søknad(
                    aktørId = AktørId(melding.getJSONObject(SØKER).getString(AKTØR_ID)),
                    mottattDato = ZonedDateTime.parse(melding.getString(MOTTATT)),
                    fødselsnummer = Fødselsnummer(melding.getJSONObject(SØKER).getString(FØDSELSNUMMER)),
                    journalpostId = hendelse.data.journalførtMelding.journalpostId,
                    søknadstype = Søknadstype.PP_SYKT_BARN,
                    status = SøknadsStatus.MOTTATT,
                    søknad = hendelse.data.melding
            )

            logger.info("Lagrer Søknad for $YTELSE")
            val søknadDAO = søknadsHendelse.tilSøknadDAO(søknadId)
            val save = repository.save(søknadDAO)
            logger.info("Søknad for $YTELSE lagret: {}", save)

            logger.info("Sender DittNav beskjed for ytelse $YTELSE")
            val k9Beskjed = byggK9Beskjed(hendelse.data.metadata, søknadId, pleiepengerDittnavBeskjedProperties, melding.getJSONObject(SØKER).getString(FØDSELSNUMMER))
            dittnavService.sendBeskjedAiven(k9Beskjed)
        }
    }
}

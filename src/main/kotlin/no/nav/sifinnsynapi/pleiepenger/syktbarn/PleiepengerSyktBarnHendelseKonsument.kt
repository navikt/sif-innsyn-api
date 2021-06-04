package no.nav.sifinnsynapi.pleiepenger.syktbarn

import no.nav.sifinnsynapi.common.*
import no.nav.sifinnsynapi.dittnav.DittnavService
import no.nav.sifinnsynapi.dittnav.K9Beskjed
import no.nav.sifinnsynapi.pleiepenger.syktbarn.PleiepengersøknadKeysV1.AKTØR_ID
import no.nav.sifinnsynapi.pleiepenger.syktbarn.PleiepengersøknadKeysV1.FØDSELSNUMMER
import no.nav.sifinnsynapi.pleiepenger.syktbarn.PleiepengersøknadKeysV1.MOTTATT
import no.nav.sifinnsynapi.pleiepenger.syktbarn.PleiepengersøknadKeysV1.SØKER
import no.nav.sifinnsynapi.pleiepenger.syktbarn.PleiepengersøknadKeysV1.SØKNAD_ID
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

    @Transactional
    @KafkaListener(
            topics = ["#{'\${topic.listener.pp-sykt-barn.navn}'}"],
            id = "#{'\${topic.listener.pp-sykt-barn.id}'}",
            groupId = "#{'\${kafka.onprem.consumer.group-id}'}",
            containerFactory = "onpremKafkaJsonListenerContainerFactory",
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
                val søknadsHendelse = Søknad(
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
            val søknadDAO = søknadsHendelse.tilSøknadDAO()
            val save = repository.save(søknadDAO)
            logger.info("Søknad for $YTELSE lagret: {}", save)

            dittnavService.sendBeskjedOnprem(
                    melding.getString(SØKNAD_ID),
                    melding.somK9Beskjed(hendelse.data.metadata, pleiepengerDittnavBeskjedProperties)
            )
        }
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

private fun JSONObject.somK9Beskjed(metadata: Metadata, beskjedProperties: PleiepengerDittnavBeskjedProperties): K9Beskjed {
    val søknadId = getString(SØKNAD_ID)
    return K9Beskjed(
            metadata = metadata,
            søkerFødselsnummer = getJSONObject(SØKER).getString(FØDSELSNUMMER),
            tekst = beskjedProperties.tekst,
            link = "${beskjedProperties.link}/$søknadId",
            grupperingsId = søknadId,
            eventId = UUID.randomUUID().toString(),
            dagerSynlig = beskjedProperties.dagerSynlig
    )
}

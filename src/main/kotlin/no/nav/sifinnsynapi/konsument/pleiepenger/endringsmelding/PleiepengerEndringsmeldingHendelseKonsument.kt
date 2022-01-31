package no.nav.sifinnsynapi.konsument.pleiepenger.endringsmelding

import no.nav.sifinnsynapi.common.*
import no.nav.sifinnsynapi.config.TxConfiguration
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
class PleiepengerEndringsmeldingHendelseKonsument(
    private val repository: SøknadRepository,
    @Value("\${topic.listener.pp-sykt-barn-endringsmelding.dry-run}") private val dryRun: Boolean
) {

    private val logger = LoggerFactory.getLogger(PleiepengerEndringsmeldingHendelseKonsument::class.java)
    private val YTELSE = "'pleiepenger - sykt barn - endringsmelding'"


    @Transactional(TxConfiguration.TRANSACTION_MANAGER)
    @KafkaListener(
        topics = ["#{'\${topic.listener.pp-sykt-barn-endringsmelding.navn}'}"],
        id = "#{'\${topic.listener.pp-sykt-barn-endringsmelding.id}'}",
        groupId = "#{'\${kafka.aiven.consumer.group-id}'}",
        containerFactory = "aivenKafkaJsonListenerContainerFactory",
        autoStartup = "#{'\${topic.listener.pp-sykt-barn-endringsmelding.bryter}'}"
    )
    fun konsumer(
        @Payload hendelse: TopicEntry
    ){
        val søknadId = hendelse.hentSøknadIdFraEndringsmelding()
        logger.info("Mottok hendelse om $YTELSE med søknadId: $søknadId")

        val søknadsHendelse = Søknad(
            aktørId = AktørId(hendelse.hentAktørIdFraEndringsmelding()),
            mottattDato = ZonedDateTime.parse(hendelse.hentMottattDatoFraEndringsmelding()),
            fødselsnummer = Fødselsnummer(hendelse.hentFødselsnummerFraEndringsmelding()),
            journalpostId = hendelse.data.journalførtMelding.journalpostId,
            søknadstype = Søknadstype.PP_SYKT_BARN_ENDRINGSMELDING,
            status = SøknadsStatus.MOTTATT,
            søknad = hendelse.data.melding
        )

        logger.info("Lagrer Søknad for $YTELSE")
        val søknadDAO = søknadsHendelse.tilSøknadDAO(søknadId)
        val save = repository.save(søknadDAO)
        logger.info("Søknad for $YTELSE lagret: {}", save)
    }
}

private fun TopicEntry.hentSøknadIdFraEndringsmelding() = JSONObject(this.data.melding).getJSONObject("k9FormatSøknad").getString("søknadId")
private fun TopicEntry.hentAktørIdFraEndringsmelding() = JSONObject(this.data.melding).getJSONObject("søker").getString("aktørId")
private fun TopicEntry.hentFødselsnummerFraEndringsmelding() = JSONObject(this.data.melding).getJSONObject("søker").getString("fødselsnummer")
private fun TopicEntry.hentMottattDatoFraEndringsmelding() = JSONObject(this.data.melding).getJSONObject("k9FormatSøknad").getString("mottattDato")
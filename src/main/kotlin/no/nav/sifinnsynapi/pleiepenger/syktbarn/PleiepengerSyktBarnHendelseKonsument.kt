package no.nav.sifinnsynapi.pleiepenger.syktbarn

import no.nav.sifinnsynapi.common.*
import no.nav.sifinnsynapi.config.TxConfiguration
import no.nav.sifinnsynapi.dittnav.DittnavService
import no.nav.sifinnsynapi.dittnav.PleiepengerDittnavBeskjedProperties
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
    }

    @Transactional(transactionManager = TxConfiguration.KAFKA_TM)
    @KafkaListener(
            topics = ["#{'\${topic.listener.pp-sykt-barn.navn}'}"],
            id = "#{'\${topic.listener.pp-sykt-barn.id}'}",
            groupId = "#{'\${spring.kafka.consumer.group-id}'}",
            containerFactory = "kafkaJsonListenerContainerFactory",
            autoStartup = "#{'\${topic.listener.pp-sykt-barn.bryter}'}"
    )
    fun konsumer(@Payload hendelse: TopicEntry) {

        if (dryRun) {
            logger.info("DRY_RUN --> Mottok hendelse fra Pleiepenger-Sykt-Barn")
            logger.info("DRY_RUN --> Mapper fra TopicEntry til Søknad for Pleiepenger-Sykt-Barn")
            try {
                val melding = JSONObject(hendelse.data.melding)
                val søknadsHendelse = Søknad(
                        aktørId = AktørId(melding.getJSONObject("søker").getString("aktørId")),
                        mottattDato = ZonedDateTime.parse(melding.getString("mottatt")),
                        fødselsnummer = Fødselsnummer(melding.getJSONObject("søker").getString("fødselsnummer")),
                        journalpostId = hendelse.data.journalførtMelding.journalpostId,
                        søknadstype = Søknadstype.PP_SYKT_BARN,
                        status = SøknadsStatus.MOTTATT,
                        søknad = hendelse.data.melding
                )
            } catch (ex: Exception) {
                logger.error("DRY_RUN --> Feilet med å mappe om TopicEntry til Søknad for Pleiepenger-Sykt-Barn")
            }
        } else {
            logger.info("Mottok hendelse fra Pleiepenger-Sykt-Barn")

            logger.info("Mapper om fra TopicEntry til Søknad for Pleiepenger-Sykt-Barn")

            val melding = JSONObject(hendelse.data.melding)
            val søknadsHendelse = Søknad(
                    aktørId = AktørId(melding.getJSONObject("søker").getString("aktørId")),
                    mottattDato = ZonedDateTime.parse(melding.getString("mottatt")),
                    fødselsnummer = Fødselsnummer(melding.getJSONObject("søker").getString("fødselsnummer")),
                    journalpostId = hendelse.data.journalførtMelding.journalpostId,
                    søknadstype = Søknadstype.PP_SYKT_BARN,
                    status = SøknadsStatus.MOTTATT,
                    søknad = hendelse.data.melding
            )

            logger.info("Lagrer Søknad fra Pleiepenger-Sykt-Barn")
            val søknadDAO = søknadsHendelse.tilSøknadDAO()
            val save = repository.save(søknadDAO)
            logger.info("Søknad for Pleiepenger-Sykt-Barn lagret: {}", save)

            dittnavService.sendBeskjed(
                    melding.getString("søknadId"),
                    melding.somK9Beskjed(hendelse.data.metadata, pleiepengerDittnavBeskjedProperties)
            )
        }
    }

    private fun Søknad.tilSøknadDAO(): SøknadDAO = SøknadDAO(
            id = UUID.fromString(søknad["søknadId"] as String),
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

data class K9Beskjed(
        val metadata: Metadata,
        val grupperingsId: String,
        val tekst: String,
        val link: String,
        val dagerSynlig: Long,
        val søkerFødselsnummer: String,
        val eventId: String
)

private fun JSONObject.somK9Beskjed(metadata: Metadata, beskjedProperties: PleiepengerDittnavBeskjedProperties): K9Beskjed {
    val søknadId = getString("søknadId")
    return K9Beskjed(
            metadata = metadata,
            søkerFødselsnummer = getJSONObject("søker").getString("fødselsnummer"),
            tekst = beskjedProperties.tekst,
            link = "${beskjedProperties.link}/$søknadId",
            grupperingsId = søknadId,
            eventId = UUID.randomUUID().toString(),
            dagerSynlig = beskjedProperties.dagerSynlig
    )
}

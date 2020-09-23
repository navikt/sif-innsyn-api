package no.nav.sifinnsynapi.pleiepenger.syktbarn

import no.nav.sifinnsynapi.common.*
import no.nav.sifinnsynapi.dittnav.DittnavService
import no.nav.sifinnsynapi.dittnav.PleiepengerDittnavBeskjedProperties
import no.nav.sifinnsynapi.soknad.Søknad
import no.nav.sifinnsynapi.soknad.SøknadRepository
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class PleiepengerSyktBarnHendelseKonsument(
        private val repository: SøknadRepository,
        private val dittnavService: DittnavService,
        private val pleiepengerDittnavBeskjedProperties: PleiepengerDittnavBeskjedProperties
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PleiepengerSyktBarnHendelseKonsument::class.java)
    }

    @KafkaListener(
            topics = ["#{'\${topic.listener.pp-sykt-barn.navn}'}"],
            id = "#{'\${topic.listener.pp-sykt-barn.id}'}",
            groupId = "#{'\${spring.kafka.consumer.group-id}'}",
            containerFactory = "kafkaJsonListenerContainerFactory",
            autoStartup = "#{'\${topic.listener.pp-sykt-barn.bryter}'}"
    )
    fun konsumer(@Payload hendelse: TopicEntry) {
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
                melding.somInnsynMelding(pleiepengerDittnavBeskjedProperties)
        )
    }
}

data class InnsynMelding(
        val grupperingsId: String,
        val tekst: String,
        val link: String,
        val dagerSynlig: Long,
        val søkerFødselsnummer: String,
        val eventId: String
)

private fun JSONObject.somInnsynMelding(beskjedProperties: PleiepengerDittnavBeskjedProperties): InnsynMelding {
    val søknadId = getString("søknadId")
    return InnsynMelding(
            søkerFødselsnummer = getJSONObject("søker").getString("fødselsnummer"),
            tekst = beskjedProperties.tekst,
            link = "${beskjedProperties.link}/$søknadId",
            grupperingsId = søknadId,
            eventId = søknadId,
            dagerSynlig = beskjedProperties.dagerSynlig
    )
}

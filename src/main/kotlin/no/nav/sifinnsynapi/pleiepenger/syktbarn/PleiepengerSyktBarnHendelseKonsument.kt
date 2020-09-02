package no.nav.sifinnsynapi.pleiepenger.syktbarn

import no.nav.brukernotifikasjon.schemas.Beskjed
import no.nav.brukernotifikasjon.schemas.Nokkel
import no.nav.sifinnsynapi.common.*
import no.nav.sifinnsynapi.config.Topics.PP_SYKT_BARN
import no.nav.sifinnsynapi.dittnav.DittnavService
import no.nav.sifinnsynapi.dittnav.PleiepengerDittnavBeskjedProperties
import no.nav.sifinnsynapi.soknad.Søknad
import no.nav.sifinnsynapi.soknad.SøknadRepository
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@Service
class PleiepengerSyktBarnHendelseKonsument(
        private val repository: SøknadRepository,
        private val dittnavService: DittnavService,
        private val pleiepengerDittnavBeskjedProperties: PleiepengerDittnavBeskjedProperties,
        @Value("\${no.nav.gateways.sts.username}") private val stsUsername: String
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PleiepengerSyktBarnHendelseKonsument::class.java)
        const val GRUPPERINGS_ID = "pleiepengesoknad-sykt-barn"
    }

    @KafkaListener(topics = [PP_SYKT_BARN], groupId = "#{'\${spring.kafka.consumer.group-id}'}", containerFactory = "kafkaJsonListenerContainerFactory")
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
                melding.somNøkkel(stsUsername),
                melding.somBeskjed(pleiepengerDittnavBeskjedProperties)
        )
    }
}

private fun JSONObject.somNøkkel(systembruker: String): Nokkel {
    return Nokkel(
            systembruker,
            getString("soknadId")
    )
}

private fun JSONObject.somBeskjed(beskjedProperties: PleiepengerDittnavBeskjedProperties): Beskjed {
    return Beskjed(
            System.currentTimeMillis(),
            Instant.now().plus(beskjedProperties.dagerSynlig.toLong(), ChronoUnit.DAYS).toEpochMilli(),
            getJSONObject("søker").getString("fødselsnummer"),
            beskjedProperties.grupperingsId,
            beskjedProperties.tekst,
            beskjedProperties.link.toString(),
            4
    )
}

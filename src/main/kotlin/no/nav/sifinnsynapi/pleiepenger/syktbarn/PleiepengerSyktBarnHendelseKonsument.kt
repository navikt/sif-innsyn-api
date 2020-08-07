package no.nav.sifinnsynapi.pleiepenger.syktbarn

import no.nav.sifinnsynapi.common.*
import no.nav.sifinnsynapi.config.Topics.PP_SYKT_BARN
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
        private val repository: SøknadRepository
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PleiepengerSyktBarnHendelseKonsument::class.java)
    }

    @KafkaListener(topics = [PP_SYKT_BARN], groupId = "#{'\${spring.kafka.consumer.group-id}'}", containerFactory = "kafkaJsonListenerContainerFactory")
    fun konsumer(@Payload hendelse: TopicEntry) {
        logger.info("Mottok hendelse fra pleiepenger sykt barn {}", hendelse)

        logger.info("Mapper om fra hendelse pleiepenger sykt barn til SøknadsHendelse...")
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

        logger.info("Lagrer søknadsHendelse for pleiepenger sykt barn...")
        val søknadDAO = søknadsHendelse.tilSøknadDAO()
        val save = repository.save(søknadDAO)
        logger.info("SøknadsHendelse for pleiepenger sykt barn lagret: {}", save)
    }
}

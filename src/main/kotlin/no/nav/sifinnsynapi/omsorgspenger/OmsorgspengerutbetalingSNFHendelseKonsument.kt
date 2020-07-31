package no.nav.sifinnsynapi.omsorgspenger

import no.nav.sifinnsynapi.common.*
import no.nav.sifinnsynapi.config.Topics.OMP_UTBETALING_SNF
import no.nav.sifinnsynapi.poc.SøknadRepository
import no.nav.sifinnsynapi.poc.SøknadsHendelse
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class OmsorgspengerutbetalingSNFHendelseKonsument(
        private val repository: SøknadRepository
) {
    companion object {
        private val logger = LoggerFactory.getLogger(OmsorgspengerutbetalingSNFHendelseKonsument::class.java)
    }

    @KafkaListener(topics = [OMP_UTBETALING_SNF], groupId = "#{'\${spring.kafka.consumer.group-id}'}", containerFactory = "kafkaJsonListenerContainerFactory")
    fun konsumer(@Payload hendelse: TopicEntry) {
        logger.info("Mottok hendelse fra omsorgspengerutbetaling SNF {}", hendelse)

        logger.info("Mapper om fra hendelse omsorgspengerutbetaling SNF til SøknadsHendelse...")
        val melding = JSONObject(hendelse.data.melding)
        val søknadsHendelse = SøknadsHendelse(
                aktørId = AktørId(melding.getJSONObject("søker").getString("aktørId")),
                mottattDato = ZonedDateTime.parse(melding.getString("mottatt")),
                fødselsnummer = Fødselsnummer(melding.getJSONObject("søker").getString("fødselsnummer")),
                journalpostId = hendelse.data.journalførtMelding.journalpostId,
                søknadstype = Søknadstype.OMP_UTBETALING_SNF,
                status = SøknadsStatus.MOTTATT,
                søknad = hendelse.data.melding
        )

        logger.info("Lagrer SøknadsHendelse...")
        val søknadDAO = søknadsHendelse.tilSøknadDAO()
        val save = repository.save(søknadDAO)
        logger.info("SøknadsHendelse lagret: {}", save)
    }
}
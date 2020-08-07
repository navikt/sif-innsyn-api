package no.nav.sifinnsynapi.omsorgspenger.utbetaling.arbeidstaker

import no.nav.sifinnsynapi.common.*
import no.nav.sifinnsynapi.config.Topics.OMP_UTBETALING_ARBEIDSTAKER
import no.nav.sifinnsynapi.soknad.Søknad
import no.nav.sifinnsynapi.soknad.SøknadRepository
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class OmsorgspengerutbetalingArbeidstakerHendelseKonsument(
        private val repository: SøknadRepository
) {
    companion object {
        private val logger = LoggerFactory.getLogger(OmsorgspengerutbetalingArbeidstakerHendelseKonsument::class.java)
    }

    @KafkaListener(topics = [OMP_UTBETALING_ARBEIDSTAKER], groupId = "#{'\${spring.kafka.consumer.group-id}'}", containerFactory = "kafkaJsonListenerContainerFactory")
    fun konsumer(@Payload hendelse: TopicEntry) {
        logger.info("Mottok hendelse fra omsorgspengerutbetaling Arbeidstaker {}", hendelse)

        logger.info("Mapper om fra hendelse omsorgspengerutbetaling Arbeidstaker til SøknadsHendelse...")
        val melding = JSONObject(hendelse.data.melding)
        val søknadsHendelse = Søknad(
                aktørId = AktørId(melding.getJSONObject("søker").getString("aktørId")),
                mottattDato = ZonedDateTime.parse(melding.getString("mottatt")),
                fødselsnummer = Fødselsnummer(melding.getJSONObject("søker").getString("fødselsnummer")),
                journalpostId = hendelse.data.journalførtMelding.journalpostId,
                søknadstype = Søknadstype.OMP_UTBETALING_ARBEIDSTAKER,
                status = SøknadsStatus.MOTTATT,
                søknad = hendelse.data.melding
        )

        logger.info("Lagrer søknadsHendelse for omsorgspengerutbetaling Arbeidstaker...")
        val søknadDAO = søknadsHendelse.tilSøknadDAO()
        val save = repository.save(søknadDAO)
        logger.info("SøknadsHendelse for omsorgspengerutbetaling Arbeidstaker lagret: {}", save)
    }
}

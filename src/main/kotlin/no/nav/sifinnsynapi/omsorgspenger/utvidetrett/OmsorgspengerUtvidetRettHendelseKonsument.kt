package no.nav.sifinnsynapi.omsorgspenger.utvidetrett

import no.nav.sifinnsynapi.common.*
import no.nav.sifinnsynapi.config.Topics.OMP_UTVIDET_RETT
import no.nav.sifinnsynapi.soknad.Søknad
import no.nav.sifinnsynapi.soknad.SøknadRepository
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class OmsorgspengerUtvidetRettHendelseKonsument(
        private val repository: SøknadRepository
) {
    companion object {
        private val logger = LoggerFactory.getLogger(OmsorgspengerUtvidetRettHendelseKonsument::class.java)
    }

    @KafkaListener(topics = [OMP_UTVIDET_RETT], groupId = "#{'\${spring.kafka.consumer.group-id}'}", containerFactory = "kafkaJsonListenerContainerFactory")
    fun konsumer(@Payload hendelse: TopicEntry) {
        logger.info("Mottok hendelse fra omsorgspenger utvidet rett {}", hendelse)

        logger.info("Mapper om fra hendelse omsorgspenger utvidet rett til SøknadsHendelse...")
        val melding = JSONObject(hendelse.data.melding)
        val søknadsHendelse = Søknad(
                aktørId = AktørId(melding.getJSONObject("søker").getString("aktørId")),
                mottattDato = ZonedDateTime.parse(melding.getString("mottatt")),
                fødselsnummer = Fødselsnummer(melding.getJSONObject("søker").getString("fødselsnummer")),
                journalpostId = hendelse.data.journalførtMelding.journalpostId,
                søknadstype = Søknadstype.OMP_UTVIDET_RETT,
                status = SøknadsStatus.MOTTATT,
                søknad = hendelse.data.melding
        )

        logger.info("Lagrer søknadsHendelse for omsorgspenger utvidet rett...")
        val søknadDAO = søknadsHendelse.tilSøknadDAO()
        val save = repository.save(søknadDAO)
        logger.info("SøknadsHendelse for omsorgspenger utvidet rett lagret: {}", save)
    }
}

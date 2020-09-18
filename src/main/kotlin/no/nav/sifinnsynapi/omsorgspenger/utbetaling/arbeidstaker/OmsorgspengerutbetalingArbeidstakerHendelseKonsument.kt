package no.nav.sifinnsynapi.omsorgspenger.utbetaling.arbeidstaker

import no.nav.sifinnsynapi.common.*
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

    @KafkaListener(
            topics = ["#{'\${topic.listener.omp-utbetaling-arbeidstaker.navn}'}"],
            id = "#{'\${topic.listener.omp-utbetaling-arbeidstaker.id}'}",
            groupId = "#{'\${spring.kafka.consumer.group-id}'}",
            containerFactory = "kafkaJsonListenerContainerFactory",
            autoStartup = "#{'\${topic.listener.omp-utbetaling-arbeidstaker.bryter}'}"
    )
    fun konsumer(@Payload hendelse: TopicEntry) {
        logger.info("Mottok hendelse fra Omsorgspenger-Utbetaling-Arbeidstaker")

        logger.info("Mapper om fra TopicEntry til Søknad for Omsorgspenger-Utbetaling-Arbeidstaker")
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

        logger.info("Lagrer Søknad fra Omsorgspenger-Utbetaling-Arbeidstaker")
        val søknadDAO = søknadsHendelse.tilSøknadDAO()
        val save = repository.save(søknadDAO)
        logger.info("Søknad for Omsorgspenger-Utbetaling-Arbeidstaker lagret: {}", save)
    }
}

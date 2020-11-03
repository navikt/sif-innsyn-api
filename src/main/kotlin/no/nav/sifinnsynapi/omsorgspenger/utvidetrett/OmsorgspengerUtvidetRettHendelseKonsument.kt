package no.nav.sifinnsynapi.omsorgspenger.utvidetrett

import no.nav.sifinnsynapi.common.*
import no.nav.sifinnsynapi.soknad.Søknad
import no.nav.sifinnsynapi.soknad.SøknadDAO
import no.nav.sifinnsynapi.soknad.SøknadRepository
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import java.time.ZonedDateTime
import java.util.*

@Service
class OmsorgspengerUtvidetRettHendelseKonsument(
        private val repository: SøknadRepository
) {
    companion object {
        private val logger = LoggerFactory.getLogger(OmsorgspengerUtvidetRettHendelseKonsument::class.java)
    }

    @KafkaListener(
            topics = ["#{'\${topic.listener.omp-utvidet-rett.navn}'}"],
            id = "#{'\${topic.listener.omp-utvidet-rett.id}'}",
            groupId = "#{'\${spring.kafka.consumer.group-id}'}",
            containerFactory = "kafkaJsonListenerContainerFactory",
            autoStartup = "#{'\${topic.listener.omp-utvidet-rett.bryter}'}"
    )
    fun konsumer(@Payload hendelse: TopicEntry) {
        logger.info("Mottok hendelse fra Omsorgspenger-Utvidet-Rett")

        logger.info("Mapper om fra TopicEntry til Søknad for Omsorgspenger-Utvidet-Rett")
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

        logger.info("Lagrer Søknad fra Omsorgspenger-Utvidet-Rett")
        val søknadDAO = søknadsHendelse.tilSøknadDAO()
        val save = repository.save(søknadDAO)
        logger.info("Søknad for Omsorgspenger-Utvidet-Rett lagret: {}", save)
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

package no.nav.sifinnsynapi.omsorgspenger.utvidetrett

import no.nav.sifinnsynapi.common.*
import no.nav.sifinnsynapi.omsorgspenger.utvidetrett.OmsorgspengerUtvidetRettHendelseKonsument.Companion.Keys.AKTØR_ID
import no.nav.sifinnsynapi.omsorgspenger.utvidetrett.OmsorgspengerUtvidetRettHendelseKonsument.Companion.Keys.FØDSELSNUMMER
import no.nav.sifinnsynapi.omsorgspenger.utvidetrett.OmsorgspengerUtvidetRettHendelseKonsument.Companion.Keys.MOTTATT
import no.nav.sifinnsynapi.omsorgspenger.utvidetrett.OmsorgspengerUtvidetRettHendelseKonsument.Companion.Keys.SØKER
import no.nav.sifinnsynapi.omsorgspenger.utvidetrett.OmsorgspengerUtvidetRettHendelseKonsument.Companion.Keys.SØKNAD_ID
import no.nav.sifinnsynapi.soknad.Søknad
import no.nav.sifinnsynapi.soknad.SøknadDAO
import no.nav.sifinnsynapi.soknad.SøknadRepository
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.*

@Service
class OmsorgspengerUtvidetRettHendelseKonsument(
        private val repository: SøknadRepository
) {
    companion object {
        private val logger = LoggerFactory.getLogger(OmsorgspengerUtvidetRettHendelseKonsument::class.java)

        private val YTELSE = "'omsorgspenger - utvidet rett'"

        private object Keys {
            const val SØKNAD_ID = "soknadId"
            const val SØKER = "søker"
            const val AKTØR_ID = "aktørId"
            const val MOTTATT = "mottatt"
            const val FØDSELSNUMMER = "fødselsnummer"
        }
    }

    @Transactional
    @KafkaListener(
            topics = ["#{'\${topic.listener.omp-utvidet-rett.navn}'}"],
            id = "#{'\${topic.listener.omp-utvidet-rett.id}'}",
            groupId = "#{'\${spring.kafka.consumer.group-id}'}",
            containerFactory = "kafkaJsonListenerContainerFactory",
            autoStartup = "#{'\${topic.listener.omp-utvidet-rett.bryter}'}"
    )
    fun konsumer(
            @Payload hendelse: TopicEntry
    ) {
        val melding = JSONObject(hendelse.data.melding)
        val søknadId = melding.getString(SØKNAD_ID)
        logger.info("Mottok hendelse om $YTELSE med søknadId: $søknadId")

        logger.info("Mapper om fra TopicEntry til Søknad for $YTELSE")
        val søknadsHendelse = Søknad(
                aktørId = AktørId(melding.getJSONObject(SØKER).getString(AKTØR_ID)),
                mottattDato = ZonedDateTime.parse(melding.getString(MOTTATT)),
                fødselsnummer = Fødselsnummer(melding.getJSONObject(SØKER).getString(FØDSELSNUMMER)),
                journalpostId = hendelse.data.journalførtMelding.journalpostId,
                søknadstype = Søknadstype.OMP_UTVIDET_RETT,
                status = SøknadsStatus.MOTTATT,
                søknad = hendelse.data.melding
        )

        logger.info("Lagrer Søknad for $YTELSE")
        val søknadDAO = søknadsHendelse.tilSøknadDAO()
        val save = repository.save(søknadDAO)
        logger.info("Søknad for $YTELSE lagret: {}", save)
    }

    private fun Søknad.tilSøknadDAO(): SøknadDAO = SøknadDAO(
            id = UUID.fromString(JSONObject(søknad).getString(SØKNAD_ID)),
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

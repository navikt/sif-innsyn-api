package no.nav.sifinnsynapi.omsorgspenger.utbetaling.snf

import no.nav.sifinnsynapi.common.*
import no.nav.sifinnsynapi.dokument.DokumentDAO
import no.nav.sifinnsynapi.dokument.DokumentRepository
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
class OmsorgspengerutbetalingSNFHendelseKonsument(
        private val søknadRepo: SøknadRepository,
        private val dokumentRepo: DokumentRepository
) {
    companion object {
        private val logger = LoggerFactory.getLogger(OmsorgspengerutbetalingSNFHendelseKonsument::class.java)
    }

    @Transactional(transactionManager = "kafkaTransactionManager")
    @KafkaListener(
            topics = ["#{'\${topic.listener.omp-utbetaling-snf.navn}'}"],
            id = "#{'\${topic.listener.omp-utbetaling-snf.id}'}",
            groupId = "#{'\${spring.kafka.consumer.group-id}'}",
            containerFactory = "kafkaJsonListenerContainerFactory",
            autoStartup = "#{'\${topic.listener.omp-utbetaling-snf.bryter}'}"
    )
    fun konsumer(@Payload hendelse: TopicEntry) {
        logger.info("Mottok hendelse fra Omsorgspenger-Utbetaling-SNF")

        logger.info("Mapper om fra TopicEntry til Søknad for Omsorgspenger-Utbetaling-SNF")

        val melding = JSONObject(hendelse.data.melding)
        val søknadsHendelse = Søknad(
                aktørId = AktørId(melding.getJSONObject("søker").getString("aktørId")),
                mottattDato = ZonedDateTime.parse(melding.getString("mottatt")),
                fødselsnummer = Fødselsnummer(melding.getJSONObject("søker").getString("fødselsnummer")),
                journalpostId = hendelse.data.journalførtMelding.journalpostId,
                søknadstype = Søknadstype.OMP_UTBETALING_SNF,
                status = SøknadsStatus.MOTTATT,
                søknad = hendelse.data.melding
        )

        logger.info("Lagrer Søknad fra Omsorgspenger-Utbetaling-SNF")
        val søknadDAO = søknadRepo.save(søknadsHendelse.tilSøknadDAO())
        logger.info("Søknad for Omsorgspenger-Utbetaling-SNF lagret: {}", søknadDAO)

        logger.info("Lagrer vedlagt pdfdokument fra Omsorgspenger-Utbetaling-SNF")
        hendelse.data.pdfDokument?.let {
            val dokumentDAO = dokumentRepo.save(DokumentDAO(
                    innhold = it,
                    søknadId = søknadDAO.id
            ))
            logger.info("Pdfdokument for Omsorgspenger-Utbetaling-SNF lagret: {}", dokumentDAO)
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

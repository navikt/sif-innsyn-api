package no.nav.sifinnsynapi.omsorgspenger

import no.nav.sifinnsynapi.common.AktørId
import no.nav.sifinnsynapi.common.Fødselsnummer
import no.nav.sifinnsynapi.common.SøknadsStatus
import no.nav.sifinnsynapi.common.Søknadstype
import no.nav.sifinnsynapi.config.Topics.OMP_UTBETALING_SNF
import no.nav.sifinnsynapi.poc.SøknadRepository
import no.nav.sifinnsynapi.poc.SøknadsHendelse
import org.hibernate.validator.internal.util.Contracts.assertNotNull
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.lang.IllegalArgumentException
import java.time.ZonedDateTime

@Service
class OmsorgspengerutbetalingSNFHendelseKonsument(
        private val repository: SøknadRepository
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(OmsorgspengerutbetalingSNFHendelseKonsument::class.java)
    }

    @Transactional
    @KafkaListener(topics = [OMP_UTBETALING_SNF], groupId = "#{'\${spring.kafka.consumer.group-id}'}", containerFactory = "kafkaJsonListenerContainerFactory")
    fun konsumer(@Payload hendelse: OmsorgspengerutbetalingSNFHendelse) {
        LOG.info("Mottok hendelse fra omsorgspengerutbetaling SNF {}", hendelse)

        LOG.info("Mapper om fra hendelse omsorgspengerutbetaling SNF til SøknadsHendelse...")
        val melding = JSONObject(hendelse.melding)
        val søknadsHendelse = SøknadsHendelse(
                aktørId = AktørId(melding.getJSONObject("søker").getString("aktørId")),
                mottattDato = ZonedDateTime.parse(melding.getString("mottatt")),
                fødselsnummer = Fødselsnummer(melding.getJSONObject("søker").getString("fødselsnummer")),
                journalpostId = hendelse.journalførtMelding.journalpostId,
                søknadstype = Søknadstype.OMP_UTBETALING_SNF,
                status = SøknadsStatus.MOTTATT,
                søknad = hendelse.melding
        )

        LOG.info("Lagrer SøknadsHendelse...")
        val søknadDAO = søknadsHendelse.tilSøknadDAO()
        val save = repository.save(søknadDAO)
        LOG.info("SøknadsHendelse lagret: {}", save)
    }
}

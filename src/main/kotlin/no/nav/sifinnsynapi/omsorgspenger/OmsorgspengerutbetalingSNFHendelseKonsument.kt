package no.nav.sifinnsynapi.omsorgspenger

import no.nav.sifinnsynapi.common.AktørId
import no.nav.sifinnsynapi.common.Fødselsnummer
import no.nav.sifinnsynapi.common.SøknadsStatus
import no.nav.sifinnsynapi.common.Søknadstype
import no.nav.sifinnsynapi.config.Topics.OMP_UTBETALING_SNF
import no.nav.sifinnsynapi.poc.SøknadsHendelse
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.stereotype.Service
import java.time.ZonedDateTime

@Service
class OmsorgspengerutbetalingSNFHendelseKonsument(
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(OmsorgspengerutbetalingSNFHendelseKonsument::class.java)
    }

    @KafkaListener(topics = [OMP_UTBETALING_SNF], groupId = "#{'\${spring.kafka.consumer.group-id}'}", containerFactory = "kafkaJsonListenerContainerFactory")
    @SendTo("INNSYN_MOTTATT")
    fun konsumer(@Payload hendelse: OmsorgspengerutbetalingSNFHendelse): SøknadsHendelse{
        LOG.info("Mottok hendelse fra omsorgspengerutbetaling SNF {}", hendelse)

        val melding = JSONObject(hendelse.melding)

        return SøknadsHendelse(
                aktørId = AktørId(melding.getJSONObject("søker").getString("aktørId")),
                mottattDato = ZonedDateTime.parse(melding.getString("mottatt")),
                fødselsnummer = Fødselsnummer(melding.getJSONObject("søker").getString("fødselsnummer")),
                journalpostId = hendelse.journalførtMelding.journalpostId,
                søknadstype = Søknadstype.OMP_UTBETALING_SNF,
                status = SøknadsStatus.MOTTATT
        )

    }

}
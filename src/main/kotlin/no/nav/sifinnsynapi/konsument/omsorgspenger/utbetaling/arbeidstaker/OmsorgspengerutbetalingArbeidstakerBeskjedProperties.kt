package no.nav.sifinnsynapi.konsument.omsorgspenger.utbetaling.arbeidstaker

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated

@ConstructorBinding
@ConfigurationProperties(prefix = "no.nav.dittnav.omsorgspengerutbetaling-arbeidstaker.beskjed")
@Validated
data class OmsorgspengerutbetalingArbeidstakerBeskjedProperties (
        val tekst: String,
        val dagerSynlig: Long,
        val link: String? = null
)

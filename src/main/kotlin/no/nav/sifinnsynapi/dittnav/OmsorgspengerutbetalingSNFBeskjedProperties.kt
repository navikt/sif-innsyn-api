package no.nav.sifinnsynapi.dittnav

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated

@ConstructorBinding
@ConfigurationProperties(prefix = "no.nav.dittnav.omsorgspengerutbetaling-snf.beskjed")
@Validated
data class OmsorgspengerutbetalingSNFBeskjedProperties (
        val tekst: String,
        val dagerSynlig: Long,
        val link: String? = null
)

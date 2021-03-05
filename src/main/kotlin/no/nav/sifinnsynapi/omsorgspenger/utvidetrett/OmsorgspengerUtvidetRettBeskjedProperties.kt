package no.nav.sifinnsynapi.omsorgspenger.utvidetrett

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated

@ConstructorBinding
@ConfigurationProperties(prefix = "no.nav.dittnav.omsorgspenger-utvidet-rett.beskjed")
@Validated
data class OmsorgspengerUtvidetRettBeskjedProperties (
        val tekst: String,
        val dagerSynlig: Long,
        val link: String? = null
)

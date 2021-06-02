package no.nav.sifinnsynapi.omsorgspenger.aleneomsorg

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated

@ConstructorBinding
@ConfigurationProperties(prefix = "no.nav.dittnav.omsorgspenger-aleneomsorg.beskjed")
@Validated
data class OmsorgspengerAleneomsorgBeskjedProperties (
        val tekst: String,
        val dagerSynlig: Long,
        val link: String? = null
)

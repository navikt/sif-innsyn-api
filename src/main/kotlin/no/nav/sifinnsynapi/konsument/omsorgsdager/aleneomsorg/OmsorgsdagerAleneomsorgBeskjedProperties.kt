package no.nav.sifinnsynapi.konsument.omsorgsdager.aleneomsorg

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated

@ConstructorBinding
@ConfigurationProperties(prefix = "no.nav.dittnav.omsorgsdager-aleneomsorg.beskjed")
@Validated
data class OmsorgsdagerAleneomsorgBeskjedProperties (
        val tekst: String,
        val dagerSynlig: Long,
        val link: String? = null
)

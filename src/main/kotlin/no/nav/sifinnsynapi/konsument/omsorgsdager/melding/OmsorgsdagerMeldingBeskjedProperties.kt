package no.nav.sifinnsynapi.konsument.omsorgsdager.melding

import no.nav.sifinnsynapi.dittnav.K9BeskjedProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated

@ConstructorBinding
@ConfigurationProperties(prefix = "no.nav.dittnav.omsorgsdager-melding-korona.beskjed")
@Validated
data class OmsorgsdagerMeldingKoronaBeskjedProperties (
        override val tekst: String,
        override val dagerSynlig: Long,
        override val link: String? = null
) : K9BeskjedProperties

@ConstructorBinding
@ConfigurationProperties(prefix = "no.nav.dittnav.omsorgsdager-melding-overfore.beskjed")
@Validated
data class OmsorgsdagerMeldingOverforeBeskjedProperties (
        override val tekst: String,
        override val dagerSynlig: Long,
        override val link: String? = null
) : K9BeskjedProperties

@ConstructorBinding
@ConfigurationProperties(prefix = "no.nav.dittnav.omsorgsdager-melding-fordele.beskjed")
@Validated
data class OmsorgsdagerMeldingFordeleBeskjedProperties (
        override val tekst: String,
        override val dagerSynlig: Long,
        override val link: String? = null
) : K9BeskjedProperties

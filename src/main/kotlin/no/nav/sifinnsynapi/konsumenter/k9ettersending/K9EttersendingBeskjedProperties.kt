@file:Suppress("ConfigurationProperties")

package no.nav.sifinnsynapi.konsumenter.k9ettersending

import no.nav.sifinnsynapi.dittnav.K9BeskjedProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated

@ConstructorBinding
@ConfigurationProperties(prefix = "no.nav.dittnav.k9-ettersending-pp.beskjed")
@Validated
data class K9EttersendingPPBeskjedProperties (
        override val tekst: String,
        override val dagerSynlig: Long,
        override val link: String? = null
) : K9BeskjedProperties


@ConstructorBinding
@ConfigurationProperties(prefix = "no.nav.dittnav.k9-ettersending-oms.beskjed")
@Validated
data class K9EttersendingOMSBeskjedProperties (
        override val tekst: String,
        override val dagerSynlig: Long,
        override val link: String? = null
) : K9BeskjedProperties
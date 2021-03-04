@file:Suppress("ConfigurationProperties")

package no.nav.sifinnsynapi.k9ettersending

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated

interface K9EttersendingBeskjedProperties{
    val tekst: String
    val dagerSynlig: Long
    val link: String?
}

@ConstructorBinding
@ConfigurationProperties(prefix = "no.nav.dittnav.k9-ettersending-pp.beskjed")
@Validated
data class K9EttersendingPPBeskjedProperties (
        override val tekst: String,
        override val dagerSynlig: Long,
        override val link: String? = null
) : K9EttersendingBeskjedProperties


@ConstructorBinding
@ConfigurationProperties(prefix = "no.nav.dittnav.k9-ettersending-oms.beskjed")
@Validated
data class K9EttersendingOMSBeskjedProperties (
        override val tekst: String,
        override val dagerSynlig: Long,
        override val link: String? = null
) : K9EttersendingBeskjedProperties
package no.nav.sifinnsynapi.konsument.pleiepenger.endringsmelding

import no.nav.sifinnsynapi.dittnav.K9BeskjedProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@ConfigurationProperties(prefix = "no.nav.dittnav.pleiepenger-sykt-barn-endringsmelding.beskjed")
@Validated
data class PleiepengerEndringsmeldingDittnavBeskjedProperties(
    override val tekst: String,
    override val link: String,
    override val dagerSynlig: Long
) : K9BeskjedProperties

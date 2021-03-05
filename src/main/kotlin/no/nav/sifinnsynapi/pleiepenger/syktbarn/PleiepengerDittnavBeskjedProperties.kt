package no.nav.sifinnsynapi.pleiepenger.syktbarn

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated
import java.net.URI

@ConstructorBinding
@ConfigurationProperties(prefix = "no.nav.dittnav.pleiepenger-sykt-barn.beskjed")
@Validated
data class PleiepengerDittnavBeskjedProperties(
        val tekst: String,
        val link: URI,
        val dagerSynlig: Long
)
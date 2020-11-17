package no.nav.sifinnsynapi.dittnav

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated
import java.net.URI

@ConstructorBinding
@ConfigurationProperties(prefix = "no.nav.dittnav.omsorgsdager-overforing.beskjed")
@Validated
data class OmsorgsdagerOverforingDittnavBedskjedProperties(
        val tekst: String,
        val link: URI,
        val dagerSynlig: Long
)


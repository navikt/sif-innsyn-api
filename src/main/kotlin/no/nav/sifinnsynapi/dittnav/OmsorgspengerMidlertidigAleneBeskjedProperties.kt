package no.nav.sifinnsynapi.dittnav

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated
import java.net.URI

@ConstructorBinding
@ConfigurationProperties(prefix = "no.nav.dittnav.omsorgspenger-midlertidig-alene.beskjed")
@Validated
data class OmsorgspengerMidlertidigAleneBeskjedProperties (
        val tekst: String,
        val dagerSynlig: Long,
        val link: URI? = null
)
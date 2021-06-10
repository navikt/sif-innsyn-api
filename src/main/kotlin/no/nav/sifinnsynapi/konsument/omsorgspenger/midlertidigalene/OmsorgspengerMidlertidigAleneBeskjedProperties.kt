package no.nav.sifinnsynapi.konsument.omsorgspenger.midlertidigalene

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated

@ConstructorBinding
@ConfigurationProperties(prefix = "no.nav.dittnav.omsorgspenger-midlertidig-alene.beskjed")
@Validated
data class OmsorgspengerMidlertidigAleneBeskjedProperties (
        val tekst: String,
        val dagerSynlig: Long,
        val link: String? = null
)

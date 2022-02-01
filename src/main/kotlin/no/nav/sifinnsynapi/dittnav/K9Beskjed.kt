package no.nav.sifinnsynapi.dittnav

import no.nav.sifinnsynapi.common.Metadata
import java.util.*

data class K9Beskjed(
        val metadata: Metadata,
        val grupperingsId: String,
        val tekst: String,
        val link: String?,
        val dagerSynlig: Long,
        val søkerFødselsnummer: String,
        val eventId: String
) {
    override fun toString(): String {
        return "K9Beskjed(metadata=$metadata, grupperingsId='$grupperingsId', tekst='$tekst', link=$link, dagerSynlig=$dagerSynlig, eventId='$eventId')"
    }
}

fun byggK9Beskjed(metadata: Metadata, søknadId: String, beskjedProperties: K9BeskjedProperties, fødselsnummer: String) = K9Beskjed(
    metadata = metadata,
    grupperingsId = søknadId,
    tekst = beskjedProperties.tekst,
    link = beskjedProperties.link,
    dagerSynlig = beskjedProperties.dagerSynlig,
    søkerFødselsnummer = fødselsnummer,
    eventId = UUID.randomUUID().toString()
)

interface K9BeskjedProperties{
    val tekst: String
    val dagerSynlig: Long
    val link: String?
}
package no.nav.sifinnsynapi.dittnav

import no.nav.sifinnsynapi.common.Metadata

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
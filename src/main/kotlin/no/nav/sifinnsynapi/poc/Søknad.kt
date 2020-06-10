package no.nav.sifinnsynapi.poc

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
data class Søknad(
        @Id @GeneratedValue val id: Long? = null,
        val språk: String,
        val harForståttRettigheterOgPlikter: Boolean,
        val harBekreftetOpplysninger: Boolean,
        val beskrivelse: String,
        val søknadstype: String
)

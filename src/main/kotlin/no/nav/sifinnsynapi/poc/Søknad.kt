package no.nav.sifinnsynapi.poc

import org.hibernate.annotations.Type
import java.util.*
import javax.persistence.Entity
import javax.persistence.Id


@Entity
data class Søknad(
        @Id
        @Type(type = "pg-uuid")
        private val id: UUID = UUID.randomUUID(),
        val språk: String,
        val harForståttRettigheterOgPlikter: Boolean,
        val harBekreftetOpplysninger: Boolean,
        val beskrivelse: String,
        val søknadstype: String
)

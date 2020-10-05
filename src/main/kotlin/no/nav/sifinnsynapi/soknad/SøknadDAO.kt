package no.nav.sifinnsynapi.soknad

import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import no.nav.sifinnsynapi.common.AktørId
import no.nav.sifinnsynapi.common.Fødselsnummer
import no.nav.sifinnsynapi.common.SøknadsStatus
import no.nav.sifinnsynapi.common.Søknadstype
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import org.hibernate.annotations.UpdateTimestamp
import org.springframework.data.annotation.CreatedDate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*
import javax.persistence.*
import javax.persistence.EnumType.STRING

@TypeDefs(
        TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)
)
@Entity(name = "søknad")
data class SøknadDAO(
        @Column(name = "id") @Id @Type(type = "pg-uuid") val id: UUID = UUID.randomUUID(),
        @Column(name = "aktør_id") @Embedded val aktørId: AktørId,
        @Column(name = "fødselsnummer") @Embedded val fødselsnummer: Fødselsnummer,
        @Column(name = "søknadstype") @Enumerated(STRING) val søknadstype: Søknadstype,
        @Column(name = "status") @Enumerated(STRING) val status: SøknadsStatus,
        @Column(name = "søknad", columnDefinition = "jsonb") @Type(type = "jsonb") val søknad: String,
        @Column(name = "saks_id") val saksId: String?,
        @Column(name = "journalpost_id") val journalpostId: String,
        @Column(name = "opprettet") @CreatedDate val opprettet: ZonedDateTime? = null,
        @Column(name = "endret") @UpdateTimestamp val endret: LocalDateTime? = null,
        @Column(name = "behandlingsdato") val behandlingsdato: LocalDate? = null
) {
    override fun toString(): String {
        return "SøknadDAO(id=$id, aktørId=$aktørId, fødselsnummer=$fødselsnummer, søknadstype=$søknadstype, status=$status, saksId=$saksId, journalpostId='$journalpostId', opprettet=$opprettet, endret=$endret, behandlingsdato=$behandlingsdato)"
    }
}

package no.nav.sifinnsynapi.poc

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
import java.util.*
import javax.persistence.*
import javax.persistence.EnumType.STRING

@TypeDefs(
        TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)
)
@Entity(name = "søknad")
data class SøknadDAO(
        @Id @Type(type = "pg-uuid") val id: UUID = UUID.randomUUID(),
        @Embedded val aktørId: AktørId,
        @Embedded val fødselsnummer: Fødselsnummer,
        @Enumerated(STRING) val søknadstype: Søknadstype,
        @Enumerated(STRING) val status: SøknadsStatus,
        @Type(type = "jsonb") @Column(columnDefinition = "jsonb") val søknad: String,
        val saksId: String,
        val journalpostId: String,
        @CreatedDate val opprettet: LocalDateTime? = null,
        @UpdateTimestamp val endret: LocalDateTime? = null,
        val behandlingsdato: LocalDate? = null
)

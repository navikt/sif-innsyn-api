package no.nav.sifinnsynapi.mikrofrontend

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import no.nav.sifinnsynapi.dittnav.MicrofrontendAction
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.type.SqlTypes
import org.springframework.data.annotation.CreatedDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(name = "mikrofrontend")
data class MikrofrontendDAO(
    @Column(name = "id") @Id @JdbcTypeCode(SqlTypes.UUID) val id: UUID = UUID.randomUUID(),
    @Column(name = "fødselsnummer", nullable = false, length = 11) val fødselsnummer: String,
    @Column(name = "mikrofrontend_id", nullable = false, length = 100) val mikrofrontendId: String,
    @Column(name = "status", nullable = false, length = 50) @Enumerated(EnumType.STRING) val status: MicrofrontendAction,
    @Column(name = "opprettet") @CreatedDate val opprettet: ZonedDateTime? = null,
    @Column(name = "endret") @UpdateTimestamp val endret: LocalDateTime? = null,
    @Column(name = "behandlingsdato") val behandlingsdato: LocalDateTime?,
)

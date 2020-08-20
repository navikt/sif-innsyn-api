package no.nav.sifinnsynapi.dokument

import org.hibernate.annotations.Type
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Lob

@Entity(name = "dokument")
data class DokumentDAO(
        @Column(name = "id") @Id @Type(type = "pg-uuid") val id: UUID = UUID.randomUUID(),
        @Lob @Column(name = "innhold") val innhold: ByteArray,
        @Column(name = "søknad_id") @Type(type = "pg-uuid") val søknadId: UUID
) {
    override fun toString(): String {
        return "DokumentDAO(id=$id, søknadId=$søknadId)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DokumentDAO

        if (id != other.id) return false
        if (!innhold.contentEquals(other.innhold)) return false
        if (søknadId != other.søknadId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + innhold.contentHashCode()
        result = 31 * result + søknadId.hashCode()
        return result
    }
}

package no.nav.sifinnsynapi.common

import com.fasterxml.jackson.annotation.JsonValue
import java.util.*
import javax.persistence.Embeddable

@Embeddable
data class AktørId(@get:JsonValue var aktørId: String? = null) {

    override fun hashCode(): Int {
        return Objects.hashCode(aktørId)
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        }
        if (obj == null) {
            return false
        }
        if (javaClass != obj.javaClass) {
            return false
        }
        val other = obj as AktørId
        if (aktørId == null) {
            if (other.aktørId != null) {
                return false
            }
        } else if (aktørId != other.aktørId) {
            return false
        }
        return true
    }

    override fun toString(): String {
        return javaClass.simpleName + "[aktørId=******]"
    }

    companion object {
        fun valueOf(aktørId: String?): AktørId {
            val id = AktørId()
            id.aktørId = aktørId
            return id
        }
    }
}

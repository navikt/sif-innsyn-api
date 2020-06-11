package no.nav.sifinnsynapi.common

import com.fasterxml.jackson.annotation.JsonValue
import com.google.common.base.Objects
import com.google.common.base.Strings
import javax.persistence.Embeddable

@Embeddable
class Fødselsnummer {
    @get:JsonValue
    var fnr: String? = null

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
        val other = obj as Fødselsnummer
        if (fnr == null) {
            if (other.fnr != null) {
                return false
            }
        } else if (fnr != other.fnr) {
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        return Objects.hashCode(fnr)
    }

    override fun toString(): String {
        return javaClass.simpleName + " [fnr=" + mask(fnr) + "]"
    }

    companion object {
        fun valueOf(fnr: String?): Fødselsnummer {
            val id = Fødselsnummer()
            id.fnr = fnr
            return id
        }

        fun mask(value: String?): String? {
            return if (value != null && value.length == 11) Strings.padEnd(value.substring(0, 6), 11, '*') else value
        }
    }
}

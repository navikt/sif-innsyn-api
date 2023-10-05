package no.nav.sifinnsynapi.dittnav

import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.Embeddable
import no.nav.sifinnsynapi.common.Metadata

data class K9Microfrontend(
    val metadata: Metadata,
    val ident: String,
    val microfrontendId: MicrofrontendId,
    val action: MicrofrontendAction,
    val sensitivitet: Sensitivitet? = null,
    val initiatedBy: String,
) {
    override fun toString(): String {
        return "K9Microfrontend(metadata=$metadata, ident='***********', microfrontendId='$microfrontendId', toggle=$action, sensitivitet=$sensitivitet)"
    }
}

enum class MicrofrontendAction {
    ENABLE, DISABLE
}

/**
 * MicrofrontendId avtales på forhånd med team-personbruker.
 */
@Embeddable
enum class MicrofrontendId(@get:JsonValue val mikrofrontendId: String) {
    PLEIEPENGER_INNSYN("pleiepenger-innsyn"),
}

enum class Sensitivitet{
    HIGH, SUBSTANTIAL;
}

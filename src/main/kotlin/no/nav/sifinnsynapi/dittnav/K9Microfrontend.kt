package no.nav.sifinnsynapi.dittnav

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
enum class MicrofrontendId(val id: String) {
    PLEIEPENGER_INNSYN("pleiepenger-innsyn");

    companion object {
        fun fromId(id: String): MicrofrontendId {
            return entries.find { it.id == id } ?: throw IllegalStateException("$id kunne ikke mappes til en enum")
        }
    }
}

enum class Sensitivitet {
    HIGH, SUBSTANTIAL;
}

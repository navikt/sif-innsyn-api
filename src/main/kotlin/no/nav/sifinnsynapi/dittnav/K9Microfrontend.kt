package no.nav.sifinnsynapi.dittnav

import no.nav.sifinnsynapi.common.Metadata

/**
 * as json
 * {
 *  "metadata": {
 *    "correlationId": "123",
 *    "version": 1
 *    },
 *    "ident": "12345678910",
 *    "microfrontendId": "pleiepenger-innsyn",
 *    "action": "ENABLE",
 *    "sensitivitet": "HIGH",
 *    "initiatedBy": "k9-dittnav-varsel"
 *    }
 *   }
 *
 */
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
    PLEIEPENGER_INNSYN("pleiepenger-innsyn"),
}

enum class Sensitivitet{
    HIGH, SUBSTANTIAL;
}

package no.nav.sifinnsynapi.mikrofrontend

import no.nav.sifinnsynapi.dittnav.MicrofrontendAction
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*
import java.util.stream.Stream


interface MikrofrontendRepository: JpaRepository<MikrofrontendDAO, UUID> {
    /**
     * Henter alle mikrofrontender basert på gitt mikrofrontendId og status.
     *
     * @param mikrofrontendId ID for mikrofrontend.
     * @param status Status for mikrofrontend.
     * @return En strøm av MikrofrontendDAO-entiteter.
     */
    fun findAllByMikrofrontendIdAndStatus(mikrofrontendId: String, status: MicrofrontendAction): Stream<MikrofrontendDAO>

    /**
     * Teller alle mikrofrontender basert på gitt mikrofrontendId og status.
     *
     * @param mikrofrontendId ID for mikrofrontend.
     * @param status Status for mikrofrontend.
     * @return Antall mikrofrontender som oppfyller kriteriene.
     */
    fun countAllByMikrofrontendIdAndStatus(mikrofrontendId: String, status: MicrofrontendAction): Long
}

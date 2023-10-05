package no.nav.sifinnsynapi.mikrofrontend

import jakarta.persistence.LockModeType
import no.nav.sifinnsynapi.dittnav.MicrofrontendAction
import no.nav.sifinnsynapi.dittnav.MicrofrontendId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import java.util.UUID
import java.util.stream.Stream


interface MikrofrontendRepository: JpaRepository<MikrofrontendDAO, UUID> {
    /**
     * Henter alle mikrofrontender basert på gitt mikrofrontendId og status med en pessimistisk leselås.
     * Når en rad er låst av denne metoden, kan ingen andre transaksjoner lese denne raden
     * før låsen er frigjort.
     *
     * @param mikrofrontendId ID for mikrofrontend.
     * @param status Status for mikrofrontend.
     * @return En strøm av MikrofrontendDAO-entiteter.
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    fun findAllByMikrofrontendIdAndStatus(mikrofrontendId: String, status: MicrofrontendAction): Stream<MikrofrontendDAO>

    /**
     * Teller alle mikrofrontender basert på gitt mikrofrontendId og status med en pessimistisk leselås.
     * Når en rad er låst av denne metoden, kan ingen andre transaksjoner lese denne raden
     * før låsen er frigjort.
     *
     * @param mikrofrontendId ID for mikrofrontend.
     * @param status Status for mikrofrontend.
     * @return Antall mikrofrontender som oppfyller kriteriene.
     */
    @Lock(LockModeType.PESSIMISTIC_READ)
    fun countAllByMikrofrontendIdAndStatus(mikrofrontendId: String, status: MicrofrontendAction): Long
}

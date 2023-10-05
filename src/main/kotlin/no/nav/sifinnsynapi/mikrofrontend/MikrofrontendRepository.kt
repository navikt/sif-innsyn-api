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
     * Henter alle mikrofrontender basert på gitt mikrofrontendId og status med en pessimistisk skrivelås.
     * Når en rad er låst av denne metoden, kan ingen andre transaksjoner lese, oppdatere eller slette denne raden
     * før låsen er frigjort.
     *
     * @param mikrofrontendId ID for mikrofrontend.
     * @param status Status for mikrofrontend.
     * @return En strøm av MikrofrontendDAO-entiteter.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findAllByMikrofrontendIdAndStatus(mikrofrontendId: MicrofrontendId, status: MicrofrontendAction): Stream<MikrofrontendDAO>

    /**
     * Teller alle mikrofrontender basert på gitt mikrofrontendId og status med en pessimistisk skrivelås.
     * Når en rad er låst av denne metoden, kan ingen andre transaksjoner lese, oppdatere eller slette denne raden
     * før låsen er frigjort.
     *
     * @param mikrofrontendId ID for mikrofrontend.
     * @param status Status for mikrofrontend.
     * @return Antall mikrofrontender som oppfyller kriteriene.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun countAllByMikrofrontendIdAndStatus(mikrofrontendId: MicrofrontendId, status: MicrofrontendAction): Long
}

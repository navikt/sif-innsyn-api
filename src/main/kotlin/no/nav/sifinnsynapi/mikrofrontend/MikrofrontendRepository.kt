package no.nav.sifinnsynapi.mikrofrontend

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*


interface MikrofrontendRepository: JpaRepository<MikrofrontendDAO, UUID> {
    /**
     * Henter alle mikrofrontender basert på gitt mikrofrontendId og status, begrenset til gitt limit.
     *
     * @param mikrofrontendId ID for mikrofrontend.
     * @param status Status for mikrofrontend.
     * @param limit Antall mikrofrontender som skal hentes.
     * @return En strøm av MikrofrontendDAO-entiteter.
     */
    @Query(
        value = """
        SELECT * FROM mikrofrontend m 
        WHERE m.mikrofrontend_id = ?1 AND m.status = ?2 
        ORDER BY m.opprettet DESC 
        LIMIT ?3
    """,
        nativeQuery = true
    )
    fun hentMikrofrontendIdAndStatus(mikrofrontendId: String, status: String, limit: Int): List<MikrofrontendDAO>


    fun existsByFødselsnummer(fødselsnummer: String): Boolean
}

package no.nav.sifinnsynapi.soknad

import no.nav.sifinnsynapi.common.AktørId
import no.nav.sifinnsynapi.config.TxConfiguration.Companion.TRANSACTION_MANAGER
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Transactional(TRANSACTION_MANAGER)
interface SøknadRepository : JpaRepository<SøknadDAO, UUID> {
    fun findAllByAktørId(aktørId: AktørId): List<SøknadDAO>
    fun findByJournalpostId(journalpostId: String): SøknadDAO?
    fun existsSøknadDAOByAktørIdAndJournalpostId(aktørId: AktørId, journalpostId: String): Boolean

    @Query(
            value = "SELECT COUNT(DISTINCT aktør_id) FROM søknad",
            nativeQuery = true
    )
    fun finnAntallUnikeSøkere(): Long

    @Query(
            value = "SELECT COUNT(*) FROM søknad WHERE søknadstype = ?1",
            nativeQuery = true
    )
    fun finnAntallSøknaderGittSøknadstype(søknadstype: String): Long
}

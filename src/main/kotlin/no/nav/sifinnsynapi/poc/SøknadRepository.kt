package no.nav.sifinnsynapi.poc

import no.nav.sifinnsynapi.common.AktørId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Transactional()
interface SøknadRepository: JpaRepository<SøknadDAO, UUID> {
    fun findAllByAktørId(aktørId: AktørId): List<SøknadDAO>
    fun findByJournalpostId(journalpostId: String): SøknadDAO?
}

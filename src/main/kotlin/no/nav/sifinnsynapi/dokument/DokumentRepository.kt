package no.nav.sifinnsynapi.dokument

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface DokumentRepository: JpaRepository<DokumentDAO, UUID> {
    fun findBySøknadId(søknadId: UUID): DokumentDAO?
}

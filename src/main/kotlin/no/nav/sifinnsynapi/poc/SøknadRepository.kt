package no.nav.sifinnsynapi.poc

import no.nav.sifinnsynapi.common.AktørId
import org.springframework.data.jpa.repository.JpaRepository

interface SøknadRepository: JpaRepository<SøknadDAO, Long> {
    fun findAllByAktørId(aktørId: AktørId): List<SøknadDAO>
}

package no.nav.sifinnsynapi.mikrofrontend

import no.nav.sifinnsynapi.dittnav.MicrofrontendAction
import no.nav.sifinnsynapi.dittnav.MicrofrontendId
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID
import java.util.stream.Stream

interface MikrofrontendRepository: JpaRepository<MikrofrontendDAO, UUID> {

    fun findAllByMikrofrontendIdAndStatus(mikrofrontendId: MicrofrontendId, status: MicrofrontendAction): Stream<MikrofrontendDAO>
    fun countAllByMikrofrontendIdAndStatus(mikrofrontendId: MicrofrontendId, status: MicrofrontendAction): Long
}

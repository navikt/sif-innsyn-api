package no.nav.sifinnsynapi.poc

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service

@Service
class SøknadService(private val repo: SøknadRepository) {

    fun hentSøknad(): List<Søknad> {
        return repo.findAll()
    }
}

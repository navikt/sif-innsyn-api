package no.nav.sifinnsynapi.poc

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service

@Service
class SøknadService(private val repo: SøknadRepository) {

    companion object {
        private val mapper = ObjectMapper()
    }

    fun hentSøknad(): List<SøknadDTO> {
        return repo.findAll().map {
            SøknadDTO(
                    saksId = it.saksId,
                    journalpostId = it.journalpostId,
                    søknadstype = it.søknadstype,
                    status = it.status,
                    opprettet = it.opprettet,
                    endret = it.endret,
                    behandlingsdato = it.behandlingsdato,
                    søknad = mapper.readValue(
                            it.søknad,
                            object :
                                    TypeReference<MutableMap<String, Any>>() {}
                    )
            )
        }
    }
}

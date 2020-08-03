package no.nav.sifinnsynapi.soknad

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.sifinnsynapi.common.AktørId
import no.nav.sifinnsynapi.oppslag.OppslagsService
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SøknadService(
        private val repo: SøknadRepository,
        val oppslagsService: OppslagsService
) {

    companion object {
        private val mapper = ObjectMapper()
    }

    fun hentSøknad(): List<SøknadDTO> {

        val aktørId = AktørId.valueOf(oppslagsService.hentAktørId()!!.aktør_id)

        return repo.findAllByAktørId(aktørId).map {
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

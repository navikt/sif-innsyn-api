package no.nav.sifinnsynapi.forvaltning

import jakarta.validation.constraints.Pattern
import no.nav.sifinnsynapi.dittnav.MicrofrontendAction
import no.nav.sifinnsynapi.mikrofrontend.MikrofrontendDAO
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

data class MikrofrontendAktiverRequest(
    @field:Pattern(regexp = "\\d{11}", message = "Fødselsnummer må være 11 siffer")
    val fødselsnummer: String,
)

data class MikrofrontendRespons(
    val id: UUID,
    val fødselsnummer: String,
    val mikrofrontendId: String,
    val status: MicrofrontendAction,
    val opprettet: ZonedDateTime?,
    val endret: LocalDateTime?,
    val behandlingsdato: LocalDateTime?,
) {
    companion object {
        fun fra(dao: MikrofrontendDAO) = MikrofrontendRespons(
            id = dao.id,
            fødselsnummer = dao.fødselsnummer,
            mikrofrontendId = dao.mikrofrontendId,
            status = dao.status,
            opprettet = dao.opprettet,
            endret = dao.endret,
            behandlingsdato = dao.behandlingsdato,
        )
    }
}

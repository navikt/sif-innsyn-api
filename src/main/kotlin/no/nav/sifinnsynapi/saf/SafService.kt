package no.nav.sifinnsynapi.saf

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.sifinnsynapi.saf.generated.HentJournalpostinfo
import no.nav.sifinnsynapi.saf.generated.hentjournalpostinfo.Journalpost
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SafService(
    private val objectMapper: ObjectMapper,
    private val safClient: GraphQLWebClient
) {
    companion object {
        private val logger = LoggerFactory.getLogger(SafService::class.java)
    }

    suspend fun hentJournalpostinfo(journalpostId: String): Journalpost {
        val response: GraphQLClientResponse<HentJournalpostinfo.Result> =
            safClient.execute(HentJournalpostinfo(HentJournalpostinfo.Variables(journalpostId)))

        return when {
            response.data!!.journalpost != null -> {
                response.data!!.journalpost!!
            }

            !response.errors.isNullOrEmpty() -> {
                val errorSomJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response.errors)
                logger.error("Feil ved henting av journalpostinfo. Årsak: {}", errorSomJson)
                throw IllegalStateException("Feil ved henting av journalpostinfo.")
            }

            else -> throw IllegalStateException("Feil ved henting av journalpostinfo.")
        }
    }
}

package no.nav.sifinnsynapi.saf

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import no.nav.sifinnsynapi.saf.generated.HentJournalpostinfo
import no.nav.sifinnsynapi.saf.generated.hentjournalpostinfo.Journalpost
import no.nav.sifinnsynapi.util.HttpHeaderConstants.X_CORRELATION_ID
import no.nav.sifinnsynapi.util.MDCUtil
import org.slf4j.LoggerFactory
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Recover
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException

@Service
@Retryable(
    exclude = [
        WebClientResponseException.Forbidden::class,
        WebClientResponseException.Unauthorized::class
    ],
    backoff = Backoff(
        delayExpression = "\${spring.rest.retry.initialDelay}",
        multiplierExpression = "\${spring.rest.retry.multiplier}",
        maxDelayExpression = "\${spring.rest.retry.maxDelay}"
    ),
    maxAttemptsExpression = "\${spring.rest.retry.maxAttempts}"
)
class SafService(
    private val objectMapper: ObjectMapper,
    private val safClient: GraphQLWebClient,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(SafService::class.java)
    }

    fun hentJournalpostinfo(journalpostId: String): Journalpost = runBlocking {
        val response: GraphQLClientResponse<HentJournalpostinfo.Result> =
            safClient.execute(HentJournalpostinfo(HentJournalpostinfo.Variables(journalpostId))) {
                header(X_CORRELATION_ID, MDCUtil.callIdOrNew())
            }

        return@runBlocking when {
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

    @Recover
    private fun recover(error: WebClientResponseException, journalpostId: String): Journalpost {
        logger.error("Feil ved henting av journalpostinfo med $journalpostId", error)
        throw IllegalStateException("Feil ved henting av journalpostinfo.")
    }

    @Recover
    private fun recover(error: IllegalStateException, journalpostId: String): Journalpost {
        logger.error("Feil ved henting av journalpostinfo med $journalpostId", error)
        throw IllegalStateException("Feil ved henting av journalpostinfo.")
    }
}

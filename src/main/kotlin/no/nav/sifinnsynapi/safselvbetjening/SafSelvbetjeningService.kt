package no.nav.sifinnsynapi.safselvbetjening

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.validation.constraints.Pattern
import kotlinx.coroutines.runBlocking
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.sifinnsynapi.safselvbetjening.generated.HentDokumentOversikt
import no.nav.sifinnsynapi.safselvbetjening.generated.hentdokumentoversikt.Dokumentoversikt
import no.nav.sifinnsynapi.util.personIdent
import org.slf4j.LoggerFactory
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpMethod
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Recover
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.validation.annotation.Validated
import org.springframework.web.client.RestTemplate
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
class SafSelvbetjeningService(
    private val objectMapper: ObjectMapper,
    private val safSelvbetjeningRestTemplate: RestTemplate,
    private val safSelvbetjeningGraphQLClient: GraphQLWebClient,
    private val tokenValidationContextHolder: SpringTokenValidationContextHolder,
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(SafSelvbetjeningService::class.java)
    }

    fun hentDokumentoversikt(): Dokumentoversikt = runBlocking {
        val personIdent = tokenValidationContextHolder.personIdent()
        val response = safSelvbetjeningGraphQLClient.execute(
            HentDokumentOversikt(
                HentDokumentOversikt.Variables(personIdent)
            )
        )

        return@runBlocking when {
            response.data != null -> response.data!!.dokumentoversiktSelvbetjening

            !response.errors.isNullOrEmpty() -> {
                val errorSomJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response.errors)
                logger.error("Feil ved henting av dokumentoversikt. Årsak: {}", errorSomJson)
                throw IllegalStateException("Feil ved henting av dokumentoversikt.")
            }

            else -> throw IllegalStateException("Feil ved henting av dokumentoversikt.")
        }
    }

    @Validated
    fun hentDokument(
        @Pattern(
            regexp = "\\d{9}",
            message = "[\${validatedValue}] matcher ikke tillatt pattern [{regexp}]"
        ) journalpostId: String,
        @Pattern(
            regexp = "\\d{9}",
            message = "[\${validatedValue}] matcher ikke tillatt pattern [{regexp}]"
        ) dokumentInfoId: String,
        @Pattern(
            regexp = "ARKIV",
            message = "[\${validatedValue}] matcher ikke tillatt pattern [{regexp}]"
        ) variantFormat: String,
    ): ArkivertDokument {
        val response = safSelvbetjeningRestTemplate.exchange(
            "/rest/hentdokument/${journalpostId}/${dokumentInfoId}/${variantFormat}",
            HttpMethod.GET,
            null,
            ByteArray::class.java
        )

        return when {
            response.statusCode.is2xxSuccessful -> ArkivertDokument(
                body = response.body!!,
                contentType = response.headers.contentType!!.type,
                contentDisposition = response.headers.contentDisposition
            )

            else -> {
                logger.error("Feilet med å hente dokument. Response: {}", response)
                throw IllegalStateException("Feilet med å hente dokument.")
            }
        }
    }

    @Recover
    private fun recoverDokumentoversikt(error: WebClientResponseException): Dokumentoversikt {
        logger.error("Feil ved henting av dokumentoversikt", error)
        throw IllegalStateException("Feil ved henting av dokumentoversikt.")
    }

    @Recover
    private fun recoverDokumentoversikt(error: IllegalStateException): Dokumentoversikt {
        logger.error("Feil ved henting av dokumentoversikt", error)
        throw IllegalStateException("Feil ved henting av dokumentoversikt.")
    }

    @Recover
    private fun recoverDokument(
        error: WebClientResponseException,
        journalpostId: String,
        dokumentInfoId: String,
        variantFormat: String,
    ): ArkivertDokument {
        logger.error(
            "Feil ved henting av dokument med journalpostId=$journalpostId, dokumentInfoId=$dokumentInfoId",
            error
        )
        throw IllegalStateException("Feilet med å hente dokument.")
    }
}

data class ArkivertDokument(
    val body: ByteArray,
    val contentType: String,
    val contentDisposition: ContentDisposition,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ArkivertDokument

        if (!body.contentEquals(other.body)) return false
        if (contentType != other.contentType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = body.contentHashCode()
        result = 31 * result + contentType.hashCode()
        return result
    }
}

package no.nav.sifinnsynapi.safselvbetjening

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.sifinnsynapi.safselvbetjening.generated.HentDokumentOversikt
import no.nav.sifinnsynapi.safselvbetjening.generated.hentdokumentoversikt.Dokumentoversikt
import org.slf4j.LoggerFactory
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class SafSelvbetjeningService(
    private val objectMapper: ObjectMapper,
    private val safSelvbetjeningRestTemplate: RestTemplate,
    private val safSelvbetjeningGraphQLClient: GraphQLWebClient,
    private val tokenValidationContextHolder: SpringTokenValidationContextHolder
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(SafSelvbetjeningService::class.java)
    }

    suspend fun hentDokumentoversikt(): Dokumentoversikt {
        val token = tokenValidationContextHolder.tokenValidationContext.firstValidToken.get()
        val response = safSelvbetjeningGraphQLClient.execute(
            HentDokumentOversikt(
                HentDokumentOversikt.Variables(token.subject)
            )
        )

        return when {
            response.data != null -> response.data!!.dokumentoversiktSelvbetjening

            !response.errors.isNullOrEmpty() -> {
                val errorSomJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response.errors)
                logger.error("Feil ved henting av dokumentoversikt. Årsak: {}", errorSomJson)
                throw IllegalStateException("Feil ved henting av dokumentoversikt.")
            }
            else -> throw IllegalStateException("Feil ved henting av dokumentoversikt.")
        }
    }

    fun hentDokument(journalpostId: String, dokumentInfoId: String, varianFormat: String): ArkivertDokument {
        return try {
            val response = safSelvbetjeningRestTemplate.exchange(
                "/rest/hentdokument/${journalpostId}/${dokumentInfoId}/${varianFormat}",
                HttpMethod.GET,
                null,
                ByteArray::class.java
            )

            when {
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
        } catch (e: Exception) {
            logger.error("Feilet med å hente dokument: {}", e)
            throw IllegalStateException("Feilet med å hente dokument.")
        }
    }
}

data class ArkivertDokument(
    val body: ByteArray,
    val contentType: String,
    val contentDisposition: ContentDisposition
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


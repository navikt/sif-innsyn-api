package no.nav.sifinnsynapi.safselvbetjening

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.sifinnsynapi.safselvbetjening.generated.HentDokumentOversikt
import no.nav.sifinnsynapi.safselvbetjening.generated.hentdokumentoversikt.Dokumentoversikt
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class SafSelvbetjeningService(
    private val objectMapper: ObjectMapper,
    private val tokenxSafSelvbetjeningClient: RestTemplate,
    private val safSelvbetjeningGraphQLClient: GraphQLWebClient
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(SafSelvbetjeningService::class.java)
    }

    suspend fun hentDokumentoversikt(norskIdentifikasjon: String): Dokumentoversikt {
        val response = safSelvbetjeningGraphQLClient.execute(
            HentDokumentOversikt(
                HentDokumentOversikt.Variables(norskIdentifikasjon)
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

    fun hentDokument(journalpostId: String, dokumentInfoId: String, varianFormat: String): String {
        val response = tokenxSafSelvbetjeningClient.exchange(
            "/rest/hentdokument/${journalpostId}/${dokumentInfoId}/${varianFormat}",
            HttpMethod.GET,
            null,
            String::class.java
        )

        return when {
            response.statusCode.is2xxSuccessful -> response.body!!
            else -> {
                logger.error("Feilet med å hente dokument. Response: {}", response)
                throw IllegalStateException("Feilet med å hente dokument.")
            }
        }
    }
}


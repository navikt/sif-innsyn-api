package no.nav.sifinnsynapi.safselvbetjening

import com.expediagroup.graphql.client.spring.GraphQLWebClient
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.sifinnsynapi.safselvbetjening.generated.HentDokumentOversikt
import no.nav.sifinnsynapi.safselvbetjening.generated.hentdokumentoversikt.Dokumentoversikt
import no.nav.sifinnsynapi.util.Constants
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.util.*

@Service
class SafSelvbetjeningService(
    private val objectMapper: ObjectMapper,
    private val tokenxSafSelvbetjeningClient: RestTemplate,
    private val safSelvbetjeningGraphQLClient: GraphQLWebClient,
    private val oAuth2AccessTokenService: OAuth2AccessTokenService,
    oauth2Config: ClientConfigurationProperties
) {

    private companion object {
        private val logger = LoggerFactory.getLogger(SafSelvbetjeningService::class.java)
    }

    private val tokenxSafSelvbetjeningClientProperties = oauth2Config.registration["tokenx-safselvbetjening"]
        ?: throw RuntimeException("could not find oauth2 client config for tokenx-safselvbetjening")

    suspend fun hentDokumentoversikt(norskIdentifikasjon: String): Dokumentoversikt {
        val accessToken =
            oAuth2AccessTokenService.getAccessToken(tokenxSafSelvbetjeningClientProperties).accessToken

        logger.info("Exchanger sluttbrukertoken mot tokenx accesstoken: {}", accessToken)

        val response = safSelvbetjeningGraphQLClient.execute(
            HentDokumentOversikt(
                HentDokumentOversikt.Variables(norskIdentifikasjon)
            )
        ) {
            header(HttpHeaders.AUTHORIZATION, accessToken)
            header(Constants.NAV_CALL_ID, UUID.randomUUID().toString())
        }

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


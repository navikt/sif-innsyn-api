package no.nav.sifinnsynapi.k9sakinnsynapi

import no.nav.k9.søknad.Søknad
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Recover
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.util.*

@Service
@Retryable(
    exclude = [HttpClientErrorException.Unauthorized::class, HttpClientErrorException.Forbidden::class, ResourceAccessException::class],
    backoff = Backoff(
        delayExpression = "\${spring.rest.retry.initialDelay}",
        multiplierExpression = "\${spring.rest.retry.multiplier}",
        maxDelayExpression = "\${spring.rest.retry.maxDelay}"
    ),
    maxAttemptsExpression = "\${spring.rest.retry.maxAttempts}"
)
class K9SakInnsynApiService(
    @Qualifier("k9SakInnsynApiClient")
    private val k9SakInnsynClient: RestTemplate
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(K9SakInnsynApiService::class.java)

        val søknadTestDataUrl = UriComponentsBuilder
            .fromUriString("/soknad/testdata")
            .build()
            .toUriString()
    }

    fun hentTestSøknader(): List<K9SakInnsynSøknad> {
        val exchange = k9SakInnsynClient.exchange(
            søknadTestDataUrl,
            HttpMethod.GET,
            null,
            object : ParameterizedTypeReference<List<K9SakInnsynSøknad>>() {})
        logger.info("Fikk response {} fra oppslag", exchange.statusCode)

        return if (exchange.statusCode.is2xxSuccessful) {
            exchange.body!!
        } else {
            logger.error("Henting av søknadsdata feilet med status: {}, og respons: {}", exchange.statusCode, exchange.body)
            throw IllegalStateException("Feilet med henting av k9 søknadsdata.")
        }
    }

    @Recover
    private fun recover(error: HttpServerErrorException): K9SakInnsynSøknad? {
        logger.error("Error response = '${error.responseBodyAsString}' fra '${søknadTestDataUrl}'")
        throw IllegalStateException("Feil ved henting av søkers personinformasjon")
    }

    @Recover
    private fun recover(error: HttpClientErrorException): K9SakInnsynSøknad? {
        logger.error("Error response = '${error.responseBodyAsString}' fra '${søknadTestDataUrl}'")
        throw IllegalStateException("Feil ved henting av søkers personinformasjon")
    }

    @Recover
    private fun recover(error: ResourceAccessException): K9SakInnsynSøknad? {
        logger.error("{}", error.message)
        throw IllegalStateException("Timout ved henting av søkers personinformasjon")
    }
}

data class K9SakInnsynSøknad(
    val søknadId: UUID,
    val søknad: Søknad
)

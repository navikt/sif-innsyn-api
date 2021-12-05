package no.nav.sifinnsynapi.k9sakinnsynapi

import no.nav.k9.søknad.Søknad
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
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

        val søknaddataUrl = UriComponentsBuilder
            .fromUriString("/soknad")
            .build()
            .toUriString()
    }

    fun hentSøknadsopplysninger(): List<K9SakInnsynSøknad> {
        val exchange = k9SakInnsynClient.exchange(
            søknaddataUrl,
            HttpMethod.GET,
            null,
            K9SakInnsynSøknader::class.java
        )
        logger.info("Fikk response {} for oppslag av søknadsdata fra k9-sak-innsyn-api", exchange.statusCode)

        return if (exchange.statusCode.is2xxSuccessful) {
            exchange.body!!.søknader
        } else {
            logger.error(
                "Henting av søknadsdata feilet med status: {}, og respons: {}",
                exchange.statusCode,
                exchange.body
            )
            throw IllegalStateException("Feilet med henting av k9 søknadsdata.")
        }
    }

    @Recover
    private fun recover(error: HttpServerErrorException): K9SakInnsynSøknader {
        logger.error("Error response = '${error.responseBodyAsString}' fra '${søknaddataUrl}'")
        throw IllegalStateException("Feilet med henting av k9 søknadsdata.")
    }

    @Recover
    private fun recover(error: HttpClientErrorException): K9SakInnsynSøknader {
        logger.error("Error response = '${error.responseBodyAsString}' fra '${søknaddataUrl}'")
        throw IllegalStateException("Feilet med henting av k9 søknadsdata.")
    }

    @Recover
    private fun recover(error: ResourceAccessException): K9SakInnsynSøknader {
        logger.error("{}", error.message)
        throw IllegalStateException("Timout ved henting av k9 søknadsdata.")
    }
}

data class K9SakInnsynSøknader(
    val søknader: List<K9SakInnsynSøknad>
)

data class K9SakInnsynSøknad(
    val pleietrengendeAktørId: String,
    val søknad: Søknad
)

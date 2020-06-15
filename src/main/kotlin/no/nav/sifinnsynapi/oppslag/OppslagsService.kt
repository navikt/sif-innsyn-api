package no.nav.sifinnsynapi.oppslag


import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Recover
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@Service
@Retryable(
        //include = [BadGateway::class, HttpServerErrorException::class],
        //exclude = [Forbidden::class],
        backoff = Backoff(
                delayExpression = "\${spring.rest.retry.initialDelay}",
                multiplierExpression = "\${spring.rest.retry.multiplier}",
                maxDelayExpression = "\${spring.rest.retry.maxDelay}"
        ),
        maxAttemptsExpression = "\${spring.rest.retry.maxAttempts}"
)
class OppslagsService(
        @Qualifier("k9OppslagsKlient")
        private val oppslagsKlient: RestTemplate
){
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(OppslagsService::class.java)

        val søkerUrl = UriComponentsBuilder
                .fromUriString("/meg")
                .queryParam("a", "aktør_id")
                .build()
    }

    fun hentAktørId(): OppslagRespons? {
        val exchange = oppslagsKlient.getForEntity(søkerUrl.toUriString(), OppslagRespons::class.java)
        logger.info("Fikk response {} fra oppslag: {}", exchange.statusCode, exchange.body)

        return exchange.body
    }

    @Recover
    fun recover(error: HttpServerErrorException): OppslagRespons? {
        logger.error("Error response = '${error.responseBodyAsString}' fra '${OppslagsService.søkerUrl.toUriString()}'")
        throw IllegalStateException("Feil ved henting av søkers personinformasjon")
    }

}

data class OppslagRespons(
        val aktør_id: String
)


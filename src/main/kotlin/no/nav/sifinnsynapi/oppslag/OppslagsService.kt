package no.nav.sifinnsynapi.oppslag

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.sifinnsynapi.util.ServletUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Recover
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.ErrorResponseException
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.Charset

@Service
@Retryable(
        exclude = [HttpClientErrorException.Forbidden::class, ResourceAccessException::class],
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
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(OppslagsService::class.java)

        val søkerUrl = UriComponentsBuilder
                .fromUriString("/meg")
                .queryParam("a", "aktør_id")
                .build()

        val søkerOppslagFeil = IllegalStateException("Timout ved henting av søkers personinformasjon")
    }

    fun hentAktørId(): OppslagRespons? {
        val exchange = oppslagsKlient.getForEntity(søkerUrl.toUriString(), OppslagRespons::class.java)
        logger.info("Fikk response {} fra oppslag", exchange.statusCode)

        return exchange.body
    }

    @Recover
    private fun recover(error: HttpServerErrorException): OppslagRespons? {
        if (error.responseBodyAsString.isNotEmpty()) logger.error("Error response = '${error.responseBodyAsString}' fra '${søkerUrl.toUriString()}'")
        else logger.error("Feil ved henting av søkers personinformasjon", error)
        throw søkerOppslagFeil
    }

    @Recover
    private fun recover(error: HttpClientErrorException): OppslagRespons? {
        if (error.statusCode == HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS) {
            throw TilgangNektetException()
        }

        if (error.responseBodyAsString.isNotEmpty()) logger.error("Error response = '${error.responseBodyAsString}' fra '${søkerUrl.toUriString()}'")
        else logger.error("Feil ved henting av søkers personinformasjon", error)
        throw søkerOppslagFeil
    }

    @Recover
    private fun recover(error: ResourceAccessException): OppslagRespons? {
        logger.error("{}", error.message)
        throw IllegalStateException("Timout ved henting av søkers personinformasjon")
    }
}

class TilgangNektetException : ErrorResponseException(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS, asProblemDetail(), null) {
    private companion object {
        private fun asProblemDetail(): ProblemDetail {
            val problemDetail = ProblemDetail.forStatus(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS)
            problemDetail.title = "Tilgang til person nektet"
            problemDetail.detail = "Tilgang til personen ble nektet fordi personen enten er under 18 år eller ikke i live."
            problemDetail.type = URI("/problem-details/tilgang-nektet")
            ServletUtils.currentHttpRequest()?.let {
                problemDetail.instance = URI(URLDecoder.decode(it.requestURL.toString(), Charset.defaultCharset()))
            }
            return problemDetail
        }
    }
}

data class OppslagRespons(@JsonProperty("aktør_id") val aktør_id: String){
    override fun toString(): String {
        return "OppslagRespons(aktør_id='******')"
    }
}

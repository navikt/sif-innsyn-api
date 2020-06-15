package no.nav.sifinnsynapi.oppslag

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@Service
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
    }

    fun hentAktørId(): OppslagRespons? {
        var exchange: ResponseEntity<Any?>? = null
        return try {
            exchange = oppslagsKlient.getForEntity(søkerUrl.toUriString(), OppslagRespons::class.java)
            logger.info("Fikk response {} fra oppslag: {}", exchange.statusCode, exchange.body)

            exchange.body
        } catch (e: RestClientException) {
            logger.error("Error response = '${exchange.asString()}' fra '${OppslagsService.søkerUrl.toUriString()}'")
            logger.error(exchange.toString())
            throw IllegalStateException("Feil ved henting av søkers personinformasjon")
        }
    }

    private fun <T> ResponseEntity<T>.asString(): String {
        return this.body.toString()
    }
}

data class OppslagRespons(
        val aktør_id: String
)


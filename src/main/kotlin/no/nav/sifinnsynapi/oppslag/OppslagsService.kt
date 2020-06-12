package no.nav.sifinnsynapi.oppslag

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@Service
class OppslagsService(
        @Qualifier("k9OppslagsKlient")
        private val oppslagsKlient: RestTemplate
) {
    private companion object {
        private val logger: Logger = LoggerFactory.getLogger(OppslagsService::class.java)
        private const val HENTE_SOKER_OPERATION = "hente-soker"
        private val objectMapper = jacksonObjectMapper().k9SelvbetjeningOppslagKonfigurert()
        private val attributter = Pair("a", listOf("aktør_id"))

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
}

data class OppslagRespons(
        val aktør_id: String
)

internal fun ObjectMapper.k9SelvbetjeningOppslagKonfigurert(): ObjectMapper {
    return ObjectMapper().apply {
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        registerModule(JavaTimeModule())
    }
}


package no.nav.sifinnsynapi.oppslag

import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService
import no.nav.security.token.support.client.spring.ClientConfigurationProperties
import no.nav.sifinnsynapi.filter.MDCValuesPropagatingClienHttpRequesInterceptor
import no.nav.sifinnsynapi.util.HttpHeaderConstants.XK9Ytelse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpRequest
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.retry.RetryCallback
import org.springframework.retry.RetryContext
import org.springframework.retry.RetryListener
import org.springframework.web.client.RestTemplate
import java.time.Duration
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime
import java.util.*

@Configuration
class OppslagsKlientKonfig(
    @Value("\${no.nav.gateways.k9-selvbetjening-oppslag}") private val oppslagsUrl: String,
    @Value("\${spring.rest.retry.maxAttempts}") private val maxAttempts: Int,
    oauth2Config: ClientConfigurationProperties,
    private val oAuth2AccessTokenService: OAuth2AccessTokenService
) : RetryListener {

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(OppslagsKlientKonfig::class.java)
        const val TOKEN_X_K9_SELVBETJENING_OPPSLAG = "tokenx-k9-selvbetjening-oppslag"
    }

    private val tokenxK9SelvbetjeningOppslagClientProperties =
        oauth2Config.registration[TOKEN_X_K9_SELVBETJENING_OPPSLAG]
            ?: throw RuntimeException("could not find oauth2 client config for $TOKEN_X_K9_SELVBETJENING_OPPSLAG")

    @Bean(name = ["k9OppslagsKlient"])
    fun restTemplate(builder: RestTemplateBuilder, mdcInterceptor: MDCValuesPropagatingClienHttpRequesInterceptor): RestTemplate {
        return builder
                .setConnectTimeout(Duration.ofSeconds(20))
                .setReadTimeout(Duration.ofSeconds(20))
                .defaultHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(XK9Ytelse, "PLEIEPENGER_SYKT_BARN")
                .rootUri(oppslagsUrl)
                .defaultMessageConverters()
                .interceptors(bearerTokenInterceptor(), mdcInterceptor)
                .build()
    }

    override fun <T : Any, E : Throwable> open(context: RetryContext, callback: RetryCallback<T, E>): Boolean {
        if (context.retryCount > 0) logger.warn("Feiler ved utgående rest-kall, kjører retry")
        return true
    }

    override fun <T : Any, E : Throwable?> close(context: RetryContext, callback: RetryCallback<T, E>, throwable: Throwable?) {
        val backoff = context.getAttribute("backOffContext")!!

        if (context.retryCount > 0) logger.info("Gir opp etter {} av {} forsøk og {} ms", context.retryCount, maxAttempts, backoff.nextInterval() - 1000)
    }

    override fun <T : Any, E : Throwable> onError(context: RetryContext, callback: RetryCallback<T, E>, throwable: Throwable) {
        val currentTry = context.retryCount
        val contextString = context.getAttribute("context.name") as String
        val backoff = context.getAttribute("backOffContext")!!
        val nextInterval = backoff.nextInterval()

        logger.warn("Forsøk {} av {}, {}", currentTry, maxAttempts, contextString.split(" ")[2])

        if (currentTry < maxAttempts) logger.info("Forsøker om: {} ms", nextInterval)
    }

    private fun bearerTokenInterceptor(): ClientHttpRequestInterceptor {
        return ClientHttpRequestInterceptor { request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution ->
            when {
                request.uri.path == "/isalive" -> {
                    // isalive skal ikke ha token
                }
                else -> {
                    val response = oAuth2AccessTokenService.getAccessToken(tokenxK9SelvbetjeningOppslagClientProperties)
                    val expiresIn = response.expiresIn
                    logger.debug("Utveklset token utgår kl. {}, ({} min)", ZonedDateTime.now(UTC).plusSeconds(expiresIn!!.toLong()), expiresIn/60)
                    request.headers.setBearerAuth(response.accessToken!!)
                }
            }
            execution.execute(request, body)
        }
    }
}

private fun Any.nextInterval(): Long {
    val getInterval = javaClass.getMethod("getInterval")
    getInterval.trySetAccessible()

    return getInterval.invoke(this) as Long
}



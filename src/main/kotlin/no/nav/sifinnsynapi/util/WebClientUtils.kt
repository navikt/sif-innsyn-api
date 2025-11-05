package no.nav.sifinnsynapi.util

import org.slf4j.Logger
import org.springframework.web.reactive.function.client.WebClient

object WebClientUtils {

    fun WebClient.Builder.requestLoggerFilter(logger: Logger) = filters { filters ->
        filters.add { request, next ->
            logger.info("---> {} {}", request.method(), request.url())
            next.exchange(request)
                .doOnNext {
                    logger.info("<--- {} for {} {}", it.statusCode().value(), request.method(), request.url())
                }
        }
    }
}

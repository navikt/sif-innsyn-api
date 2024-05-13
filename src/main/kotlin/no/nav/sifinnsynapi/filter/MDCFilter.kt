package no.nav.sifinnsynapi.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpFilter
import jakarta.servlet.http.HttpServletRequest
import no.nav.sifinnsynapi.util.CallIdGenerator
import no.nav.sifinnsynapi.util.HttpHeaderConstants.NAV_CALL_ID
import no.nav.sifinnsynapi.util.HttpHeaderConstants.X_CORRELATION_ID
import no.nav.sifinnsynapi.util.MDCConstants.CORRELATION_ID
import no.nav.sifinnsynapi.util.MDCConstants.NAV_CONSUMER_ID
import no.nav.sifinnsynapi.util.MDCUtil
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import java.io.IOException

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class MDCFilter(
    private val generator: CallIdGenerator,
    @Value("\${spring.application.name:sif-innsyn-api}") private val applicationName: String
) : HttpFilter() {

    companion object {
        private val logger = LoggerFactory.getLogger(MDCFilter::class.java)
    }

    @Throws(IOException::class, ServletException::class)
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        putValues(request as HttpServletRequest)
        chain.doFilter(request, response)
    }

    private fun putValues(req: HttpServletRequest) {
        try {
            MDCUtil.toMDC(NAV_CONSUMER_ID, req.getHeader(NAV_CONSUMER_ID), applicationName)
            MDCUtil.toMDC(CORRELATION_ID, req.getHeader(X_CORRELATION_ID), generator.create())
        } catch (e: Exception) {
            logger.warn("Feil ved setting av MDC-verdier for {}, MDC-verdier er inkomplette", req.requestURI, e)
        }
    }

    override fun toString(): String {
        return javaClass.simpleName + " [generator=" + generator + ", applicationName=" + applicationName + "]"
    }
}

@Component
class HeadersToMDCFilterRegistrationBean(headersFilter: MDCFilter) : FilterRegistrationBean<MDCFilter>() {
    companion object {
        private val logger = LoggerFactory.getLogger(HeadersToMDCFilterRegistrationBean::class.java)
    }

    init {
        filter = headersFilter
        urlPatterns = FilterRegistrationUtil.always()
        logger.info("Registrert filter {}", this.javaClass.simpleName)
    }
}

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
class MDCValuesPropagatingClienHttpRequesInterceptor : ClientHttpRequestInterceptor {

    @Throws(IOException::class)
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {
        MDC.get(CORRELATION_ID)?.apply { request.headers.add(X_CORRELATION_ID, this) }
        MDC.get(NAV_CONSUMER_ID)?.apply { request.headers.add(NAV_CALL_ID, this) }
        return execution.execute(request, body)
    }
}

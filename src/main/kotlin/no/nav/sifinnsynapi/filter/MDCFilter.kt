package no.nav.sifinnsynapi.filter

import no.nav.sifinnsynapi.util.CallIdGenerator
import no.nav.sifinnsynapi.util.Constants
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
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpFilter
import javax.servlet.http.HttpServletRequest

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
            MDCUtil.toMDC(Constants.NAV_CONSUMER_ID, req.getHeader(Constants.NAV_CONSUMER_ID), applicationName)
            MDCUtil.toMDC(Constants.CORRELATION_ID, req.getHeader(Constants.CORRELATION_ID), generator.create())
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
    override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
        propagerFraMDC(request, Constants.CORRELATION_ID, Constants.NAV_CONSUMER_ID)
        return execution.execute(request, body)
    }

    companion object {
        private fun propagerFraMDC(request: HttpRequest, vararg keys: String) {
            keys.forEach { key ->
                val value = MDC.get(key)
                if (value != null) {
                    request.headers.add(key, value)
                }
            }
            request.headers.add(Constants.CALL_ID, MDCUtil.callId())
        }
    }
}

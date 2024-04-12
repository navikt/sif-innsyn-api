package no.nav.sifinnsynapi.filter

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView

@Component
class LoggerInterceptor(private val tokenValidationContextHolder: TokenValidationContextHolder) : HandlerInterceptor {
    private companion object {
        private val logger = LoggerFactory.getLogger(LoggerInterceptor::class.java)
    }
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val jwtToken = tokenValidationContextHolder.getTokenValidationContext().firstValidToken
        if (jwtToken !== null) {
            logger.info("Issuer [${jwtToken.issuer}]")
        }
        val method = request.method
        val requestURI = request.requestURI
        logger.info("Request $method $requestURI")
        return super.preHandle(request, response, handler)
    }

    override fun postHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        modelAndView: ModelAndView?
    ) {
        val status = response.status
        val method = request.method
        val requestURI = request.requestURI
        logger.info("Response $status $method $requestURI")
        super.postHandle(request, response, handler, modelAndView)
    }
}

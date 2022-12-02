package no.nav.sifinnsynapi.util

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.context.request.ServletWebRequest
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.Charset

object ServletUtils {

    fun currentHttpRequest(): HttpServletRequest? {
        val requestAttributes = RequestContextHolder.getRequestAttributes()
        return if (requestAttributes is ServletRequestAttributes) {
            requestAttributes.request
        } else null
    }

    fun ServletWebRequest.respondProblemDetails(
        status: HttpStatus,
        title: String,
        type: URI,
        properties: Map<String, Any> = mapOf(),
        detail: String,
    ): ProblemDetail {
        val problemDetail = ProblemDetail.forStatusAndDetail(status, detail)
        problemDetail.title = title
        problemDetail.type = type
        problemDetail.instance = URI(URLDecoder.decode(request.requestURL.toString(), Charset.defaultCharset()))
        properties.forEach {
            problemDetail.setProperty(it.key, it.value)
        }
        return problemDetail
    }
}

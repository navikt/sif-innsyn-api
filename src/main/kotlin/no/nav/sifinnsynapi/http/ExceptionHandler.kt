package no.nav.sifinnsynapi.http

import jakarta.validation.ConstraintViolationException
import no.nav.security.token.support.core.exceptions.JwtTokenMissingException
import no.nav.security.token.support.core.exceptions.JwtTokenValidatorException
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import no.nav.sifinnsynapi.util.ServletUtils.respondProblemDetails
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.net.URI

@ControllerAdvice
class ExceptionHandler : ResponseEntityExceptionHandler() {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ExceptionHandler::class.java)
    }

    @ExceptionHandler(value = [Exception::class])
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun håndtereGeneriskException(exception: Exception, request: ServletWebRequest): ProblemDetail {
        val problemDetails = request.respondProblemDetails(
            status = HttpStatus.INTERNAL_SERVER_ERROR,
            title = "Et uventet feil har oppstått",
            type = URI("/problem-details/internal-server-error"),
            detail = exception.message ?: ""
        )
        log.error("{}", problemDetails)
        return problemDetails
    }

    @ExceptionHandler(value = [ConstraintViolationException::class])
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun håndtereConstraintViolationException(
        exception: ConstraintViolationException,
        request: ServletWebRequest,
    ): ProblemDetail {
        val problemDetails = request.respondProblemDetails(
            status = HttpStatus.BAD_REQUEST,
            title = "Ugyldig forespørsel",
            type = URI("/problem-details/ugyldig-forespørsel"),
            detail = "Forespørselen inneholder valideringsfeil",
            properties = mapOf(
                "violations" to exception.constraintViolations
                    .sortedBy { it.propertyPath.toString()}
                    .map {
                        mapOf(
                            "property" to it.propertyPath.toString(),
                            "message" to it.message,
                            "invalidValue" to it.invalidValue,
                        )
                    }
            )
        )
        log.error("{}", problemDetails)
        return problemDetails
    }


    @ExceptionHandler(value = [JwtTokenUnauthorizedException::class])
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun håndtereTokenUnauthorizedException(
        exception: JwtTokenUnauthorizedException,
        request: ServletWebRequest,
    ): ProblemDetail {
        val problemDetails = request.respondProblemDetails(
            status = HttpStatus.UNAUTHORIZED,
            title = "Ikke autentisert",
            type = URI("/problem-details/uautentisert-forespørsel"),
            detail = exception.message ?: ""
        )
        log.debug("{}", problemDetails)
        return problemDetails
    }

    @ExceptionHandler(JwtTokenValidatorException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun håndtereTokenUnauthenticatedException(
        exception: JwtTokenValidatorException,
        request: ServletWebRequest,
    ): ProblemDetail {
        val problemDetails = request.respondProblemDetails(
            status = HttpStatus.FORBIDDEN,
            title = "Ikke uautorisert",
            type = URI("/problem-details/uautorisert-forespørsel"),
            detail = exception.message ?: ""
        )
        log.debug("{}", problemDetails)
        return problemDetails
    }

    @ExceptionHandler(JwtTokenMissingException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun håndtereJwtTokenMissingException(
        exception: JwtTokenMissingException,
        request: ServletWebRequest,
    ): ProblemDetail {
        val problemDetails = request.respondProblemDetails(
            status = HttpStatus.UNAUTHORIZED,
            title = "Ingen token funnet.",
            type = URI("/problem-details/mangler-token"),
            detail = exception.message ?: ""
        )
        log.debug("{}", problemDetails)
        return problemDetails
    }
}

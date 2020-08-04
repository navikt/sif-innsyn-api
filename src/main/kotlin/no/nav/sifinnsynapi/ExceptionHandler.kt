package no.nav.sifinnsynapi

import no.nav.security.token.support.core.exceptions.JwtTokenValidatorException
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import no.nav.sifinnsynapi.http.problem.UnAuthorizedRequestProblem
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.context.request.ServletWebRequest
import org.springframework.web.context.request.WebRequest
import org.zalando.problem.Problem
import org.zalando.problem.Status
import org.zalando.problem.spring.web.advice.ProblemHandling
import java.net.URI

@ControllerAdvice
class ExceptionHandler: ProblemHandling {

    companion object{
        private val logger: Logger = LoggerFactory.getLogger(ExceptionHandler::class.java)
    }

    @ExceptionHandler(value = [Exception::class])
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun håndtereGeneriskException(exception: Exception, request: ServletWebRequest): ResponseEntity<Problem> {
        log(HttpStatus.INTERNAL_SERVER_ERROR, exception, request)
        return create(Status.INTERNAL_SERVER_ERROR, exception, request, URI("/problem-details/internal-server-error"))
    }

    @ExceptionHandler(value = [JwtTokenUnauthorizedException::class])
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun håndtereTokenUnauthorizedException(exception: JwtTokenUnauthorizedException, request: ServletWebRequest): ResponseEntity<Problem>{

        log(HttpStatus.UNAUTHORIZED, exception, request)

        val unAuthorizedRequestProblem = UnAuthorizedRequestProblem(
                type = URI("/problem-details/unauthorized"),
                status = Status.UNAUTHORIZED,
                title = "Ikke autentisert",
                detail = "Forespørsel med gitt token er ikke autentisert."
        )
        return create(Status.UNAUTHORIZED, unAuthorizedRequestProblem, request, URI("/problem-details/unauthorized"))
    }

    @ExceptionHandler(JwtTokenValidatorException::class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    fun håndtereTokenUnauthenticatedException(exception: Exception, request: ServletWebRequest): ResponseEntity<Any?>? {
        log(HttpStatus.FORBIDDEN, exception, request)
        return ResponseEntity("Token unauthenticated", HttpHeaders(), HttpStatus.FORBIDDEN)
    }

    fun log(status: HttpStatus, exception: Exception, request: WebRequest){
        logger.error("{} - {}, {}", status, exception, request.toString())
    }

}

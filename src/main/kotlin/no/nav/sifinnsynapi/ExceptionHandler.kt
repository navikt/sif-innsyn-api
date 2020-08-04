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
    fun håndtereGeneriskException(exception: Exception, request: ServletWebRequest): ResponseEntity<Problem> {
        log(HttpStatus.INTERNAL_SERVER_ERROR, exception, request)
        return create(Status.INTERNAL_SERVER_ERROR, exception, request, URI("/problem-details/internal-server-error"))
    }

    @ExceptionHandler(value = [NoHandlerFoundException::class])
    @ResponseStatus(value= HttpStatus.NOT_FOUND)
    fun håndtere404Exception(exception: Exception, request: WebRequest?): ResponseEntity<Any> {
        log(HttpStatus.UNAUTHORIZED, exception, request)
        return ResponseEntity("404 - Not found feilmelding", HttpHeaders(), HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(value = [JwtTokenUnauthorizedException::class])
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
    fun håndtereTokenUnauthenticatedException(exception: Exception, request: ServletWebRequest): ResponseEntity<Any?>? {
        log(HttpStatus.FORBIDDEN, exception, request)
        return ResponseEntity("Token unauthenticated", HttpHeaders(), HttpStatus.FORBIDDEN)
    }

    fun log(status: HttpStatus, exception: Exception, request: WebRequest){
        logger.error("{} - {}, {}", status, exception, request.toString())
    }

}

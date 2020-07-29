package no.nav.sifinnsynapi

import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.NoHandlerFoundException

@ControllerAdvice
class ExceptionHandler {

    companion object{
        private val logger: Logger = LoggerFactory.getLogger(ExceptionHandler::class.java)
    }

    @ExceptionHandler(value = [Exception::class])
    fun håndtereGeneriskException(exception: Exception, request: WebRequest?): ResponseEntity<Any> {
        return logAndHandle(HttpStatus.UNAUTHORIZED, exception, request)
    }

    @ExceptionHandler(value = [NoHandlerFoundException::class])
    @ResponseStatus(value= HttpStatus.NOT_FOUND)
    fun håndtere404Exception(exception: Exception, request: WebRequest?): ResponseEntity<Any> {
        return logAndHandle(HttpStatus.UNAUTHORIZED, exception, request)
    }

    @ExceptionHandler(value = [JwtTokenUnauthorizedException::class])
    fun håndtereTokenUnauthorizedException(exception: Exception, request: WebRequest?): ResponseEntity<Any>{
        return logAndHandle(HttpStatus.UNAUTHORIZED, exception, request)
    }

    fun logAndHandle(status: HttpStatus, exception: Exception, request: WebRequest?): ResponseEntity<Any>{
        logger.error("{} - {}, {}", status, exception, request.toString())
        return ResponseEntity(status)
    }

}
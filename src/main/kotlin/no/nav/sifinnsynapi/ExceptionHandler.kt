package no.nav.sifinnsynapi

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.NoHandlerFoundException

@ControllerAdvice
class ExceptionHandler {

    companion object{
        private val log: Logger = LoggerFactory.getLogger(ExceptionHandler::class.java)
    }

    @ExceptionHandler(value = [Exception::class])
    fun handleAnyException(ex: Exception, request: WebRequest?): ResponseEntity<Any> {
        log.error("Exception kastet -------> {}, {}, {}, {}", ex.message, HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request.toString())

        return ResponseEntity("Beklager, her skjedde det en feil", HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(value = [NoHandlerFoundException::class])
    fun handle404Exception(ex: Exception, request: WebRequest?): ResponseEntity<Any> {
        log.error("Exception kastet -------> {}, {}, {}, {}", ex.message, HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request.toString())

        return ResponseEntity("Beklager, her skjedde det en feil. 404", HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
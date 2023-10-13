package no.nav.sifinnsynapi.exception

import org.slf4j.LoggerFactory

class SifInnsynApiUncaughtExceptionHandler : Thread.UncaughtExceptionHandler {
    private companion object {
        private val logger = LoggerFactory.getLogger(SifInnsynApiUncaughtExceptionHandler::class.java)
    }
    override fun uncaughtException(t: Thread, e: Throwable) {
        // Handle the uncaught exception here
        logger.error("Uncaught exception in thread ${t.name}", e)
    }
}

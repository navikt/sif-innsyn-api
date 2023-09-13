package no.nav.sifinnsynapi.dittnav

import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@Unprotected
class DittnavController(private val dittnavService: DittnavService) {

    @PostMapping("/dittnav/microfrontend/toggle")
    @ResponseStatus(HttpStatus.OK)
    @Transactional
    fun toggleMicrofrontend(@RequestBody k9Microfrontend: K9Microfrontend) {
        dittnavService.toggleMicrofrontend(k9Microfrontend)
    }
}

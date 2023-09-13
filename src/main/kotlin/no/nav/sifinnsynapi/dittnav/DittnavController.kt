package no.nav.sifinnsynapi.dittnav

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController("/dittnav")
class DittnavController(private val dittnavService: DittnavService) {

    @PostMapping("/microfrontend/toggle")
    @ResponseStatus(HttpStatus.OK)
    fun toggleMicrofrontend(@RequestBody k9Microfrontend: K9Microfrontend) {
        dittnavService.toggleMicrofrontend(k9Microfrontend)
    }
}

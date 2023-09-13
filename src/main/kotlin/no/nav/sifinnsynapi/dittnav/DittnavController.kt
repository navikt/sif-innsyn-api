package no.nav.sifinnsynapi.dittnav

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController("/dittnav")
class DittnavController(private val dittnavService: DittnavService) {

    @PostMapping("/microfrontend/toggle")
    fun toggleMicrofrontend(k9Microfrontend: K9Microfrontend) {
        dittnavService.toggleMicrofrontend(k9Microfrontend)
    }
}

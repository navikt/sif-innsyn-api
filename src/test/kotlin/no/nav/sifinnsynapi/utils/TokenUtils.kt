package no.nav.sifinnsynapi.utils

import com.nimbusds.jwt.SignedJWT
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders

fun SignedJWT.tokenSomHttpEntity(): HttpEntity<String> = tokenTilHttpEntity()

fun SignedJWT.tokenSomHttpHeader(): HttpHeaders = tokenTilHeader()

private fun SignedJWT.tokenTilHttpEntity(): HttpEntity<String> {
    val headers = this.tokenTilHeader()
    return HttpEntity<String>(headers)
}

private fun SignedJWT.tokenTilHeader(): HttpHeaders {
    val token = serialize()
    val headers = HttpHeaders()
    headers.setBearerAuth(token)
    return headers
}

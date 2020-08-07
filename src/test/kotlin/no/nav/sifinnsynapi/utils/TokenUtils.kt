package no.nav.sifinnsynapi.utils

import com.nimbusds.jwt.SignedJWT
import no.nav.security.token.support.test.JwtTokenGenerator
import no.nav.sifinnsynapi.common.Fødselsnummer
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders

fun tokenSomHttpEntity(fødselsnummer: Fødselsnummer): HttpEntity<String>{
    return JwtTokenGenerator.createSignedJWT(fødselsnummer.fødselsnummer).tokenTilHttpEntity()
}

fun tokenSomHttpHeader(fødselsnummer: Fødselsnummer): HttpHeaders{
    return JwtTokenGenerator.createSignedJWT(fødselsnummer.fødselsnummer).tokenTilHeader()
}

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
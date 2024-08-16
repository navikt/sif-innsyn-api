package no.nav.sifinnsynapi.sikkerhet

import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.springframework.web.context.request.RequestContextHolder

class ContextHolder private constructor(private val context: SpringTokenValidationContextHolder) {

    companion object {
        const val AZURE_AD = "azure"
        private var instans: ContextHolder? = null
        val INSTANCE: ContextHolder
            get() {
                if (instans == null) {
                    instans = ContextHolder(SpringTokenValidationContextHolder())
                }
                return instans!!
            }

    }

    fun requestKontekst(): RequestKontekst? {
        if (RequestContextHolder.getRequestAttributes() == null)
            return null

        val tokenContext = context.getTokenValidationContext()
        val reqIssuerShortNames = tokenContext.issuers //alle issuers på alle validerte tokens i context
        if (reqIssuerShortNames.contains(AZURE_AD)) {
            val jwtToken: JwtToken? = tokenContext.getJwtToken(AZURE_AD)
            return jwtToken?.let { RequestKontekst(it, AZURE_AD) }
        }
        return null
    }

    @JvmRecord
    data class RequestKontekst(val jwtToken: JwtToken, val issuerShortname: String)


}

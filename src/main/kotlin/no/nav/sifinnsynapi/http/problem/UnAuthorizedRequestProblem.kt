package no.nav.sifinnsynapi.http.problem

import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.web.bind.annotation.ResponseStatus
import org.zalando.problem.AbstractThrowableProblem
import org.zalando.problem.Exceptional
import org.zalando.problem.StatusType
import org.zalando.problem.ThrowableProblem
import java.net.URI

@ResponseStatus(UNAUTHORIZED)
class UnAuthorizedRequestProblem(
        type: URI,
        title: String?,
        status: StatusType?,
        detail: String?) :
        AbstractThrowableProblem(
                type,
                title,
                status,
                detail
        ) {
    override fun getCause(): Exceptional {
        TODO("Not yet implemented")
    }
}

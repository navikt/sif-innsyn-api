package no.nav.sifinnsynapi.common

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class Metadata @JsonCreator constructor(
        @JsonProperty("version") val version : Int,
        @JsonProperty("correlationId") val correlationId : String,
        @JsonProperty("requestId") val requestId : String? = null
)

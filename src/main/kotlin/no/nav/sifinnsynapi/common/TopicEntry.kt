package no.nav.sifinnsynapi.common

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.sifinnsynapi.omsorgspenger.OmsorgspengerutbetalingSNFHendelse

data class TopicEntry @JsonCreator constructor(
        @JsonProperty("data") val data: OmsorgspengerutbetalingSNFHendelse
)
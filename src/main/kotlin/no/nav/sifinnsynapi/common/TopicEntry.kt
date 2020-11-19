package no.nav.sifinnsynapi.common

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class TopicEntry @JsonCreator constructor(
        @JsonProperty("data") val data: SøknadsHendelse
)

data class ConsumerRecordData @JsonCreator constructor(
        @JsonProperty("data") val data: Data
)

data class Data @JsonCreator constructor(
        @JsonProperty("metadata") val metadata: Metadata
)



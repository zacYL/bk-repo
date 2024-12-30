package com.tencent.bkrepo.oci.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Manifest(val config: Layer, val layers: List<Layer>) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Layer(val digest: String)

}
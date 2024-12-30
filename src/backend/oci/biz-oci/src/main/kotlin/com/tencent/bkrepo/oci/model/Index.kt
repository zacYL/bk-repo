package com.tencent.bkrepo.oci.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Index(val manifests: List<Manifest>) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Manifest(val digest: String)

}
package com.tencent.bkrepo.docker.model

import com.google.common.collect.Multimap

data class DockerUploadResult(
    val dockerDigest:DockerDigest,
    val size:Long,
    val labels:Multimap<String, String>,
    val metadata: Map<String, String> ?= emptyMap()
)

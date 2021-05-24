package com.tencent.bkrepo.docker.pojo

import com.tencent.bkrepo.docker.response.DockerResponse

data class DockerResponseWithTag(
    val tag: String,
    val dockerResponse: DockerResponse
)
package com.tencent.bkrepo.maven.pojo.response

data class MavenWebDeployResponse(
        val uuid: String,
        val groupId: String,
        val artifactId: String,
        val version: String,
        val classifier: String?,
        val type: String
)
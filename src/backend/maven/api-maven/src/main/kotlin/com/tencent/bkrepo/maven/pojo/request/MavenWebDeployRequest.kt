package com.tencent.bkrepo.maven.pojo.request

data class MavenWebDeployRequest(
    val uuid: String,
    val groupId: String?,
    val artifactId: String?,
    val version: String?,
    val classifier: String?,
    val type: String?
)
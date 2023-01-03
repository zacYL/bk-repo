package com.tencent.bkrepo.maven.pojo

data class MavenDependency(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val type: String,
    val scope: String?,
    val classifier: String?,
    val optional: Boolean?,
)

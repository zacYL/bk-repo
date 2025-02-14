package com.tencent.bkrepo.maven.pojo

data class MavenDependentsReverse(
        val projectId: String,
        val repoName: String,
        val packageKey: String,
        val groupId: String,
        val artifactId: String,
        val version: String,
        val type: String,
        val classifier: String
)
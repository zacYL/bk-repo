package com.tencent.bkrepo.docker.service

interface DockerDeleteService {
    fun deleteVersion(
        projectId: String,
        repoName: String,
        packageKey: String,
        version: String,
        operator: String
    ): Boolean
}
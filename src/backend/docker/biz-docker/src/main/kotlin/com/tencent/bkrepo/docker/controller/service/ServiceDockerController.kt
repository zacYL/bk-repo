package com.tencent.bkrepo.docker.controller.service

import com.tencent.bkrepo.docker.api.DockerClient
import com.tencent.bkrepo.docker.service.DockerDeleteService
import org.springframework.web.bind.annotation.RestController

@RestController
class ServiceDockerController(
    private val dockerDeleteService: DockerDeleteService
) : DockerClient {

    override fun deleteVersion(
        projectId: String,
        repoName: String,
        packageKey: String,
        version: String,
        operator: String
    ): Boolean {
        return  dockerDeleteService.deleteVersion(projectId, repoName, packageKey, version, operator)
    }
}
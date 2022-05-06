package com.tencent.bkrepo.repository.service.artifact

import com.tencent.bkrepo.docker.api.DockerClient
import com.tencent.bkrepo.repository.constant.SYSTEM_USER

class DockerClientService(
    private val dockerClient: DockerClient
) : ArtifactClientService{
    override fun deleteVersion(projectId: String, repoName: String, packageKey: String, version: String) {
        dockerClient.deleteVersion(projectId,repoName,packageKey,version, SYSTEM_USER)
    }

}

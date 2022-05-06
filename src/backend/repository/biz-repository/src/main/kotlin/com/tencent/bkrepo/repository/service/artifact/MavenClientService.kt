package com.tencent.bkrepo.repository.service.artifact

import com.tencent.bkrepo.maven.api.MavenClient
import com.tencent.bkrepo.repository.constant.SYSTEM_USER
import org.springframework.stereotype.Service

@Service
class MavenClientService(
    private val mavenClient: MavenClient
) : ArtifactClientService{
    override fun deleteVersion(projectId: String, repoName: String, packageKey: String, version: String) {
        mavenClient.deleteVersion(projectId, repoName, packageKey, version, SYSTEM_USER)
    }
}

package com.tencent.bkrepo.repository.service.artifact

import com.tencent.bkrepo.common.api.constant.REPO_CLEAN_USER
import com.tencent.bkrepo.maven.api.MavenClient
import org.springframework.stereotype.Service

@Service
class MavenArtifactRegistryService(
    private val mavenClient: MavenClient
) : ArtifactRegistryService{
    override fun deleteVersion(projectId: String, repoName: String, packageKey: String, version: String) {
        mavenClient.deleteVersion(projectId, repoName, packageKey, version, REPO_CLEAN_USER)
    }
}
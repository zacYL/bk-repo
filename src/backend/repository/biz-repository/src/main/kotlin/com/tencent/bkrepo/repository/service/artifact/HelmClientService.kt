package com.tencent.bkrepo.repository.service.artifact

import com.tencent.bkrepo.helm.api.HelmClient
import com.tencent.bkrepo.repository.constant.SYSTEM_USER
import org.springframework.stereotype.Service

@Service
class HelmClientService(
    private val helmClient: HelmClient
) : ArtifactClientService {
    override fun deleteVersion(projectId: String, repoName: String, packageKey: String, version: String) {
        helmClient.deleteVersion(projectId, repoName, packageKey, version, SYSTEM_USER)
    }
}

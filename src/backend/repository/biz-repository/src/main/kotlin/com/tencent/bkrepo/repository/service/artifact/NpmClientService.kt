package com.tencent.bkrepo.repository.service.artifact

import com.tencent.bkrepo.npm.api.NpmClient
import com.tencent.bkrepo.repository.constant.SYSTEM_USER
import org.springframework.stereotype.Service

@Service
class NpmClientService(
    private val npmClient: NpmClient
) : ArtifactClientService {
    override fun deleteVersion(projectId: String, repoName: String, packageKey: String, version: String) {
        npmClient.deleteVersion(projectId,repoName,packageKey,version, SYSTEM_USER)
    }

}

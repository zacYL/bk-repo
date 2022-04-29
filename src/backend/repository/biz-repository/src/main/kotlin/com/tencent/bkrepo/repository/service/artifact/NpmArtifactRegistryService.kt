package com.tencent.bkrepo.repository.service.artifact

import com.tencent.bkrepo.npm.api.NpmResource
import org.springframework.stereotype.Service

@Service
class NpmArtifactRegistryService(
    //private val npmResource: NpmResource
) : ArtifactRegistryService {
    override fun deleteVersion(projectId: String, repoName: String, packageKey: String, version: String) {
        TODO("Not yet implemented")
    }

}
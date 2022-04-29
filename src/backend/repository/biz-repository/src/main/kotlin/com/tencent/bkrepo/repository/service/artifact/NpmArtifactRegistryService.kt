package com.tencent.bkrepo.repository.service.artifact

import org.springframework.stereotype.Service

@Service
class NpmArtifactRegistryService(
    //private val npmResource: NpmResource
) : ArtifactRegistryService {
    override fun deleteVersion(projectId: String, repoName: String, packageKey: String, version: String) {
        TODO("Not yet implemented")
    }

}
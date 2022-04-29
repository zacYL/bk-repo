package com.tencent.bkrepo.repository.service.artifact

interface ArtifactRegistryService {
    fun deleteVersion(projectId:String,repoName:String,packageKey:String,version:String)
}
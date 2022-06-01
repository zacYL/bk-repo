package com.tencent.bkrepo.repository.service.artifact

interface ArtifactClientService {
    fun deleteVersion(projectId:String,repoName:String,packageKey:String,version:String)
}

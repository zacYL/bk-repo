package com.tencent.bkrepo.nuget.service

import com.tencent.bkrepo.nuget.pojo.artifact.NugetDeleteArtifactInfo
import com.tencent.bkrepo.nuget.pojo.artifact.NugetPublishArtifactInfo

interface NugetClientService {

    /**
     * push nuget package
     */
    fun publish(userId: String, publishInfo: NugetPublishArtifactInfo)

    /**
     * delete nupkg with version
     */
    fun delete(userId: String, artifactInfo: NugetDeleteArtifactInfo)
}

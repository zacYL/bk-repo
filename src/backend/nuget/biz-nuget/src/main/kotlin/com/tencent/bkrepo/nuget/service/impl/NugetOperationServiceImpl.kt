package com.tencent.bkrepo.nuget.service.impl

import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.nuget.pojo.artifact.NugetDownloadArtifactInfo
import com.tencent.bkrepo.nuget.service.NugetOperationService
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import org.springframework.stereotype.Service

@Service
class NugetOperationServiceImpl(
    private val packageClient: PackageClient
) : NugetOperationService {
    override fun packageVersion(context: ArtifactContext): PackageVersion? {
        with(context.artifactInfo as NugetDownloadArtifactInfo) {
            if (version.isEmpty()) return null
            val packageKey = PackageKeys.ofNuget(packageName)
            return packageClient.findVersionByName(projectId, repoName, packageKey, version).data
        }
    }
}

package com.tencent.bkrepo.npm.service.impl

import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.npm.constants.NPM_FILE_FULL_PATH
import com.tencent.bkrepo.npm.service.NpmOperationService
import com.tencent.bkrepo.npm.utils.NpmUtils
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import org.springframework.stereotype.Service

@Service
class NpmOperationServiceImpl(
    private val packageClient: PackageClient
) : NpmOperationService {
    override fun packageVersion(context: ArtifactContext): Pair<String, PackageVersion>? {
        with(context) {
            val (packageName, packageVersion) = if (context is ArtifactUploadContext) {
                NpmUtils.parseNameAndVersionFromFullPath(getAttributes()[NPM_FILE_FULL_PATH] as String)
            } else {
                NpmUtils.parseNameAndVersionFromFullPath(artifactInfo.getArtifactFullPath())
            }
            val packageKey = PackageKeys.ofNpm(packageName)
            return packageClient.findVersionByName(projectId, repoName, packageKey, packageVersion).data
                ?.let { Pair(packageKey, it) }
        }
    }
}

package com.tencent.bkrepo.maven.service.impl

import com.tencent.bkrepo.common.metadata.service.packages.PackageService
import com.tencent.bkrepo.common.metadata.util.PackageKeys
import com.tencent.bkrepo.maven.constants.METADATA_KEY_ARTIFACT_ID
import com.tencent.bkrepo.maven.constants.METADATA_KEY_GROUP_ID
import com.tencent.bkrepo.maven.constants.METADATA_KEY_VERSION
import com.tencent.bkrepo.maven.service.MavenOperationService
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import org.springframework.stereotype.Service

@Service
class MavenOperationServiceImpl(
    private val packageService: PackageService
) : MavenOperationService {
    override fun packageVersion(node: NodeDetail): PackageVersion? {
        val groupId = node.metadata[METADATA_KEY_GROUP_ID]?.toString()
        val artifactId = node.metadata[METADATA_KEY_ARTIFACT_ID]?.toString()
        val version = node.metadata[METADATA_KEY_VERSION]?.toString()
        return if (groupId != null && artifactId != null && version != null) {
            val packageKey = PackageKeys.ofGav(groupId, artifactId)
            packageService.findVersionByName(node.projectId, node.repoName, packageKey, version)
        } else {
            null
        }
    }
}

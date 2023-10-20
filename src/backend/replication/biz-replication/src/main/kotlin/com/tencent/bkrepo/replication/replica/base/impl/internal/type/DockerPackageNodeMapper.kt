package com.tencent.bkrepo.replication.replica.base.impl.internal.type

import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.replication.constant.OCI_LAYER_FULL_PATH
import com.tencent.bkrepo.replication.constant.OCI_LIST_MANIFEST_JSON_FULL_PATH
import com.tencent.bkrepo.replication.constant.OCI_MANIFEST_JSON_FULL_PATH
import com.tencent.bkrepo.replication.constant.OCI_MANIFEST_LIST
import com.tencent.bkrepo.replication.util.ManifestParser
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.pojo.packages.PackageSummary
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import org.springframework.stereotype.Component

/**
 * DOCKER 依赖源需要迁移manifest.json文件以及该文件内容里面包含的config文件和layers文件
 */
@Component
class DockerPackageNodeMapper(
    private val nodeClient: NodeClient,
    private val storageService: StorageService,
    private val repositoryClient: RepositoryClient
) : PackageNodeMapper {

    override fun type() = RepositoryType.DOCKER
    override fun extraType(): RepositoryType? {
        return RepositoryType.OCI
    }

    override fun map(
        packageSummary: PackageSummary,
        packageVersion: PackageVersion,
        type: RepositoryType
    ): List<String> {
        with(packageSummary) {
            val result = mutableListOf<String>()
            val name = packageSummary.name
            val version = packageVersion.name
            val repository = repositoryClient.getRepoDetail(projectId, repoName, type.name).data!!
            // 旧的docker数据需要迁移，迁移后不需要兼容
            var manifestFullPath = OCI_MANIFEST_JSON_FULL_PATH.format(name, version)
            val nodeDetail = nodeClient.getNodeDetail(projectId, repoName, manifestFullPath).data ?: run {
                // 兼容 list.manifest.json
                manifestFullPath = OCI_LIST_MANIFEST_JSON_FULL_PATH.format(name, version)
                nodeClient.getNodeDetail(projectId, repoName, manifestFullPath).data!!
            }
            val inputStream = storageService.load(
                nodeDetail.sha256.orEmpty(),
                Range.full(nodeDetail.size),
                repository.storageCredentials
            )!!
            // list.manifest.json 只需要分发本身
            if (nodeDetail.name != OCI_MANIFEST_LIST) {
                val manifestInfo = ManifestParser.parseManifest(inputStream) ?: return result
                manifestInfo.descriptors?.forEach {
                    val replace = it.replace(":", "__")
                    result.add(OCI_LAYER_FULL_PATH.format(name, version, replace))
                }
            }
            result.add(manifestFullPath)
            return result
        }
    }
}

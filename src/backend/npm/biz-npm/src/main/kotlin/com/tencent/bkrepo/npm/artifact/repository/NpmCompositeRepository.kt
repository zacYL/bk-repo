package com.tencent.bkrepo.npm.artifact.repository

import com.tencent.bkrepo.common.api.util.JsonUtils
import com.tencent.bkrepo.common.artifact.constant.SOURCE_TYPE
import com.tencent.bkrepo.common.artifact.exception.ArtifactNotInWhitelistException
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.repository.composite.CompositeRepository
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.local.LocalRepository
import com.tencent.bkrepo.common.artifact.repository.remote.RemoteRepository
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactChannel
import com.tencent.bkrepo.npm.constants.NPM_FILE_FULL_PATH
import com.tencent.bkrepo.npm.model.metadata.NpmPackageMetaData
import com.tencent.bkrepo.npm.utils.NpmUtils
import com.tencent.bkrepo.repository.api.ProxyChannelClient
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import java.io.InputStream

/**
 * 使用自定义的bean对象替换到composite上层实现
 */
@Component
@Primary
class NpmCompositeRepository(
    private val localRepository: LocalRepository,
    private val remoteRepository: RemoteRepository,
    proxyChannelClient: ProxyChannelClient
) : CompositeRepository(localRepository, remoteRepository, proxyChannelClient) {

    override fun whitelistInterceptor(context: ArtifactDownloadContext) {
        if (whitelistSwitchClient.get(RepositoryType.NPM).data == true) {
            val fullPath = context.getStringAttribute(NPM_FILE_FULL_PATH)?: return
            logger.info("npm local fullPath: $fullPath")
            val packageInfo = NpmUtils.parseNameAndVersionFromFullPath(context.artifactInfo.getArtifactFullPath())
            logger.info("npm local packageInfo: [${packageInfo.first} : ${packageInfo.second}]")
            nodeClient.getNodeDetail(
                    projectId = context.projectId,
                    repoName = context.repoName,
                    fullPath = fullPath).data?.let { nodeDetail ->
                // 如果节点在仓库中存在
                logger.info("npm local nodeDetail: $nodeDetail")
                nodeDetail.nodeMetadata.forEach { metadataModel ->
                    // 判断是否来自代理源缓存
                    if (metadataModel.key == SOURCE_TYPE && metadataModel.value == ArtifactChannel.PROXY.name
                            // 查询该制品是否在白名单中
                            && remotePackageClient.search(
                                    RepositoryType.NPM, packageInfo.first, packageInfo.second
                            ).data != true) {
                        // 抛出异常 Http.status = 423
                        throw ArtifactNotInWhitelistException()
                    }
                }
            }
        }
    }

    override fun query(context: ArtifactQueryContext): InputStream? {
        val localQueryResult = localRepository.query(context) as? InputStream
        val remoteQueryResult = mapFirstProxyRepo(context) {
            require(it is ArtifactQueryContext)
            remoteRepository.query(it) as? InputStream
        } ?: return localQueryResult

        localQueryResult?.use { it ->
            // 将远程结果与本地结果合并进行返回
            val localPackageMetaData = JsonUtils.objectMapper.readValue(it, NpmPackageMetaData::class.java)
            val remotePackageMetaData = remoteQueryResult.use {
                JsonUtils.objectMapper.readValue(it, NpmPackageMetaData::class.java)
            }
            remotePackageMetaData.versions.map.putAll(localPackageMetaData.versions.map)
            val packageMetadata =
                JsonUtils.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(remotePackageMetaData)
            val artifactFile = ArtifactFileFactory.build(packageMetadata.byteInputStream())
            return artifactFile.getInputStream()
        }
        return remoteQueryResult
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NpmCompositeRepository::class.java)
    }
}

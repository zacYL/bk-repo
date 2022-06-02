package com.tencent.bkrepo.npm.artifact.repository

import com.tencent.bkrepo.common.api.util.JsonUtils
import com.tencent.bkrepo.common.artifact.repository.composite.CompositeRepository
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.local.LocalRepository
import com.tencent.bkrepo.common.artifact.repository.remote.RemoteRepository
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.npm.model.metadata.NpmPackageMetaData
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

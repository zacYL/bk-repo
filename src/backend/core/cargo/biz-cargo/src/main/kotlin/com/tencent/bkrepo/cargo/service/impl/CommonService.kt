/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.cargo.service.impl

import com.tencent.bkrepo.cargo.pojo.artifact.CargoArtifactInfo
import com.tencent.bkrepo.cargo.pojo.artifact.CargoDeleteArtifactInfo
import com.tencent.bkrepo.cargo.pojo.index.CrateIndex
import com.tencent.bkrepo.cargo.pojo.json.CrateJsonData
import com.tencent.bkrepo.cargo.service.impl.CargoExtServiceImpl.Companion.PAGE_SIZE
import com.tencent.bkrepo.cargo.utils.CargoUtils.getCargoFileFolder
import com.tencent.bkrepo.cargo.utils.CargoUtils.getCargoFileFullPath
import com.tencent.bkrepo.cargo.utils.CargoUtils.getCargoIndexFullPath
import com.tencent.bkrepo.cargo.utils.CargoUtils.getCargoJsonFullPath
import com.tencent.bkrepo.cargo.utils.ObjectBuilderUtil.buildMetadataSaveRequest
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.util.JsonUtils
import com.tencent.bkrepo.common.api.util.jsonCompress
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.exception.RepoNotFoundException
import com.tencent.bkrepo.common.artifact.exception.VersionNotFoundException
import com.tencent.bkrepo.common.artifact.manager.StorageManager
import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.stream.ArtifactInputStream
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.lock.service.LockOperation
import com.tencent.bkrepo.common.metadata.model.TNode
import com.tencent.bkrepo.common.metadata.service.metadata.MetadataService
import com.tencent.bkrepo.common.metadata.service.node.NodeSearchService
import com.tencent.bkrepo.common.metadata.service.node.NodeService
import com.tencent.bkrepo.common.metadata.service.packages.PackageService
import com.tencent.bkrepo.common.metadata.service.repo.RepositoryService
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.repository.pojo.download.PackageDownloadRecord
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import com.tencent.bkrepo.repository.pojo.packages.VersionListOption
import com.tencent.bkrepo.repository.pojo.search.NodeQueryBuilder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.InputStream

@Component
class CommonService {

    @Autowired
    lateinit var lockOperation: LockOperation

    @Autowired
    lateinit var storageManager: StorageManager

    @Autowired
    lateinit var repositoryService: RepositoryService

    @Autowired
    lateinit var nodeService: NodeService

    @Autowired
    lateinit var packageService: PackageService

    @Autowired
    lateinit var metadataService: MetadataService

    @Autowired
    lateinit var nodeSearchService: NodeSearchService

    /**
     * 针对自旋达到次数后，还没有获取到锁的情况默认也会执行所传入的方法,确保业务流程不中断
     */
    fun <T> lockAction(projectId: String, repoName: String, path: String, action: () -> T): T {
        val lockKey = buildRedisKey(projectId, repoName, path)
        val lock = lockOperation.getLock(lockKey)
        return if (lockOperation.getSpinLock(lockKey, lock)) {
            logger.info("Lock for key cargo $lockKey has been acquired.")
            try {
                action()
            } finally {
                lockOperation.close(lockKey, lock)
                logger.info("Lock for cargo key $lockKey has been released.")
            }
        } else {
            action()
        }
    }

    fun getStreamOfCrate(projectId: String, repoName: String, fullPath: String): ArtifactInputStream? {
        val nodeDetail = nodeService.getNodeDetail(ArtifactInfo(projectId, repoName, fullPath))
        val storageCredentials = getStorageCredentials(projectId, repoName)
        return storageManager.loadArtifactInputStream(nodeDetail, storageCredentials)
    }

    fun getIndexOfCrate(projectId: String, repoName: String, crateName: String): MutableList<CrateIndex> {
        val fullPath = getCargoIndexFullPath(crateName)
        val inputStream = getStreamOfCrate(projectId, repoName, fullPath)
        return convertToCrateIndex(inputStream)
    }

    fun getJsonOfCrate(projectId: String, repoName: String, crateName: String, version: String): CrateJsonData? {
        val fullPath = getCargoJsonFullPath(crateName, version)
        val inputStream = getStreamOfCrate(projectId, repoName, fullPath)
        return inputStream?.use {
            JsonUtils.objectMapper.readValue(it, CrateJsonData::class.java)
        }
    }

    fun convertToCrateIndex(inputStream: InputStream?): MutableList<CrateIndex> {
        return inputStream?.use {
            // 读取 InputStream 的内容
            val lines = it.bufferedReader().readLines()
            // 解析为对象，并去重
            return lines.asSequence()
                .mapNotNull { line ->
                    try {
                        JsonUtils.objectMapper.readValue(line, CrateIndex::class.java)
                    } catch (e: Exception) {
                        // 记录日志或忽略无效行
                        null
                    }
                }
                .toMutableList()
        } ?: mutableListOf()
    }

    fun buildIndexArtifactFile(
        versions: MutableList<CrateIndex>, storageCredentials: StorageCredentials?
    ): ArtifactFile {
        val updatedLines = versions.joinToString("\n") { crateIndex ->
            // 将对象序列化为紧凑的 JSON 字符串
            JsonUtils.objectMapper.writeValueAsString(crateIndex).jsonCompress()
        }
        return ArtifactFileFactory.build(updatedLines.byteInputStream(), storageCredentials = storageCredentials)
    }

    fun getStorageCredentials(projectId: String, repoName: String): StorageCredentials? {
        val repository = repositoryService.getRepoDetail(projectId, repoName, RepositoryType.CARGO.name)
            ?: throw RepoNotFoundException("Repository[$repoName] does not exist")
        return repository.storageCredentials
    }

    /**
     * upload index of crate
     */
    fun uploadIndexOfCrate(artifactFile: ArtifactFile, nodeCreateRequest: NodeCreateRequest) {
        val repository = repositoryService.getRepoDetail(
            nodeCreateRequest.projectId,
            nodeCreateRequest.repoName,
            RepositoryType.CARGO.name
        )
            ?: throw RepoNotFoundException("Repository[${nodeCreateRequest.repoName}] does not exist")
        storageManager.storeArtifactFile(nodeCreateRequest, artifactFile, repository.storageCredentials)
    }

    fun getNodeDetail(projectId: String, repoName: String, fullPath: String): NodeDetail? {
        return nodeService.getNodeDetail(ArtifactInfo(projectId, repoName, fullPath))
    }

    fun findVersionByName(projectId: String, repoName: String, key: String, version: String): PackageVersion? {
        return packageService.findVersionByName(projectId, repoName, key, version)
    }

    /**
     * check package [key] version [version] exists
     */
    fun packageVersionExist(projectId: String, repoName: String, key: String, version: String): Boolean {
        return findVersionByName(projectId, repoName, key, version)?.let { true } ?: false
    }

    /**
     * check package [key] exists
     */
    fun packageExist(projectId: String, repoName: String, key: String): Boolean {
        return packageService.findPackageByKey(projectId, repoName, key)?.let { true } ?: false
    }

    fun removeCargoRelatedNode(context: ArtifactRemoveContext) {
        with(context.artifactInfo as CargoDeleteArtifactInfo) {
            if (version.isNotBlank()) {
                packageService.findVersionByName(projectId, repoName, packageName, version)?.let {
                    removeVersion(
                        projectId = projectId,
                        repoName = repoName,
                        packageKey = packageName,
                        version = it.name,
                        userId = context.userId
                    )
                } ?: throw VersionNotFoundException(version)
            } else {
                packageService.listAllVersion(projectId, repoName, packageName, VersionListOption()).forEach {
                    removeVersion(
                        projectId = projectId,
                        repoName = repoName,
                        packageKey = packageName,
                        version = it.name,
                        userId = context.userId
                    )
                }
                packageService.deletePackage(projectId, repoName, packageName)
            }
        }
    }

    /**
     * 删除[version] 对应的node节点也会一起删除
     */
    fun removeVersion(
        projectId: String,
        repoName: String,
        version: String,
        packageKey: String,
        userId: String
    ) {
        packageService.deleteVersion(projectId, repoName, packageKey, version)
        val packageName = PackageKeys.resolveCargo(packageKey)
        val deletedNodes = listOf(
            getCargoFileFullPath(packageName, version),
            getCargoJsonFullPath(packageName, version)
        )
        deletedNodes.forEach {
            if (it.isNotBlank()) {
                val request = NodeDeleteRequest(projectId, repoName, it, userId)
                nodeService.deleteNode(request)
            }
        }
    }

    fun updateNodeMetaData(
        projectId: String,
        repoName: String,
        fullPath: String,
        metadata: MutableMap<String, Any>? = null
    ) {
        val metadataSaveRequest = buildMetadataSaveRequest(
            projectId = projectId,
            repoName = repoName,
            fullPath = fullPath,
            metadata = metadata,
            userId = SecurityUtils.getUserId()
        )
        try {
            metadataService.saveMetadata(metadataSaveRequest)
        } catch (ignore: Exception) {
        }
    }

    fun queryCrateNodesByCrateName(
        projectId: String,
        repoName: String,
        crateName: String,
        pageNum: Int
    ): Page<Map<String, Any?>> {
        val cargoFileFolder = getCargoFileFolder(crateName)
        val queryModelBuilder = NodeQueryBuilder()
            .select(TNode::metadata.name, TNode::sha256.name)
            .sortByAsc(TNode::name.name)
            .page(pageNum, PAGE_SIZE)
            .projectId(projectId)
            .repoName(repoName).path(PathUtils.toPath(cargoFileFolder))
        return nodeSearchService.searchWithoutCount(queryModelBuilder.build())
    }

    fun buildDownloadRecord(
        userId: String,
        artifactInfo: CargoArtifactInfo,
    ): PackageDownloadRecord? {
        with(artifactInfo) {
            if (crateName.isNullOrEmpty() || crateVersion.isNullOrEmpty()) {
                return null
            }
            return PackageDownloadRecord(projectId, repoName, PackageKeys.ofCargo(crateName), crateVersion)
        }
    }

    private fun buildRedisKey(projectId: String, repoName: String, revPath: String): String {
        return "$REDIS_LOCK_KEY_PREFIX$projectId/$repoName/$revPath"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CommonService::class.java)
        const val REDIS_LOCK_KEY_PREFIX = "cargo:lock:index:"

    }
}
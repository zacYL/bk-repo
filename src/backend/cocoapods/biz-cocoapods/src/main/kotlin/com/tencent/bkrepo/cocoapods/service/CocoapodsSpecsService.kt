/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.cocoapods.service

import ArchiveModifier
import com.tencent.bkrepo.cocoapods.artifact.CocoapodsProperties
import com.tencent.bkrepo.cocoapods.constant.DOT_SPECS
import com.tencent.bkrepo.cocoapods.constant.LOCK_PREFIX
import com.tencent.bkrepo.cocoapods.dao.CocoapodsGitInstanceDao
import com.tencent.bkrepo.cocoapods.dao.CocoapodsRemotePackageDao
import com.tencent.bkrepo.cocoapods.exception.CocoapodsMessageCode
import com.tencent.bkrepo.cocoapods.exception.CocoapodsCommonException
import com.tencent.bkrepo.cocoapods.model.TCocoapodsGitInstance
import com.tencent.bkrepo.cocoapods.model.TCocoapodsRemotePackage
import com.tencent.bkrepo.cocoapods.pojo.CocoapodsRemoteConfiguration
import com.tencent.bkrepo.cocoapods.pojo.enums.RemoteRepoType
import com.tencent.bkrepo.cocoapods.utils.DecompressUtil.buildEmptySpecGzOps
import com.tencent.bkrepo.cocoapods.utils.FileUtil
import com.tencent.bkrepo.cocoapods.utils.GitUtil
import com.tencent.bkrepo.cocoapods.utils.ObjectBuildUtil.toCocoapodsRemoteConfiguration
import com.tencent.bkrepo.cocoapods.utils.PathUtil
import com.tencent.bkrepo.cocoapods.utils.PathUtil.generateIndexTarPath
import com.tencent.bkrepo.common.api.constant.ADMIN_USER
import com.tencent.bkrepo.common.api.constant.CharPool.SLASH
import com.tencent.bkrepo.common.api.constant.HttpHeaders
import com.tencent.bkrepo.common.api.constant.MediaTypes.APPLICATION_GZIP
import com.tencent.bkrepo.common.api.constant.MediaTypes.APPLICATION_ZIP
import com.tencent.bkrepo.common.api.constant.ensurePrefix
import com.tencent.bkrepo.common.artifact.exception.ArtifactNotFoundException
import com.tencent.bkrepo.common.artifact.exception.NodeNotFoundException
import com.tencent.bkrepo.common.artifact.exception.RepoNotFoundException
import com.tencent.bkrepo.common.artifact.manager.StorageManager
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.pojo.RepositoryIdentify
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.pojo.configuration.remote.RemoteConfiguration
import com.tencent.bkrepo.common.artifact.repository.remote.buildOkHttpClient
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResourceWriterContext
import com.tencent.bkrepo.common.artifact.util.http.HttpHeaderUtils.encodeDisposition
import com.tencent.bkrepo.common.lock.service.LockOperation
import com.tencent.bkrepo.common.redis.RedisOperation
import com.tencent.bkrepo.common.service.util.HttpContextHolder.getResponse
import com.tencent.bkrepo.common.storage.core.StorageProperties
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.NodeListOption
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepositoryInfo
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import javax.servlet.http.HttpServletResponse

@Service
class CocoapodsSpecsService(
    private val nodeClient: NodeClient,
    private val repositoryClient: RepositoryClient,
    private val storageManager: StorageManager,
    private val storageProperties: StorageProperties,
    private val cocoapodsProperties: CocoapodsProperties,
    private val lockOperation: LockOperation,
    private val redisOperation: RedisOperation,
    private val cocoapodsRemotePackageDao: CocoapodsRemotePackageDao,
    private val cocoapodsGitInstanceDao: CocoapodsGitInstanceDao,
    private val artifactResourceWriterContext: ArtifactResourceWriterContext,
) {

    fun initSpecs(projectId: String, repoName: String) {
        logger.info("project [$projectId], repo [$repoName],init specs...")
        nodeClient.createNode(NodeCreateRequest(
            projectId = projectId,
            repoName = repoName,
            fullPath = DOT_SPECS,
            folder = true
        ))
    }

    fun initRemoteSpecs(projectId: String, repoInfo: RepositoryInfo) {
        logger.info("project [$projectId], repo [${repoInfo.name}],init remote specs...")
        val conf = repoInfo.configuration as RemoteConfiguration
        val cocoapodsConf = conf.toCocoapodsRemoteConfiguration()
        val outputStream = ByteArrayOutputStream()
        val tempPath = FileUtil.buildTempDir("cocoapods/$projectId/${repoInfo.name}")
        val lockKey = "$LOCK_PREFIX:init_specs:$projectId:${repoInfo.name}"
         lockOperation.doWithLock(lockKey) {
             val podspecList = when (cocoapodsConf.type) {
                RemoteRepoType.GIT_HUB -> {
                    cloneAndGetPodSpecs(conf, projectId, repoInfo, tempPath, outputStream)
                }
                else -> {
                    downloadAndGetPodSpecs(conf, cocoapodsConf, repoInfo, projectId, outputStream, tempPath)
                }
            }
             val repoDetail = repositoryClient.getRepoDetail(projectId, repoInfo.name).data
                 ?: throw RepoNotFoundException(repoInfo.name)

             val specArtifact = ArtifactFileFactory.build(outputStream.toByteArray().inputStream(), repoDetail.storageCredentials
                 ?: storageProperties.defaultStorageCredentials())
             val nodeCreateRequest = NodeCreateRequest(
                 projectId = projectId,
                 repoName = repoInfo.name,
                 fullPath = generateIndexTarPath(),
                 folder = false,
                 size = specArtifact.getSize(),
                 sha256 = specArtifact.getFileSha256(),
                 md5 = specArtifact.getFileMd5(),
                 overwrite = false,
                 operator = ADMIN_USER
             )
             storageManager.storeArtifactFile(nodeCreateRequest, specArtifact, null)
             cocoapodsRemotePackageDao.insert(podspecList.map {
                 TCocoapodsRemotePackage(
                     projectId = projectId,
                     repoName = repoInfo.name,
                     packageName = it.name,
                     packageVersion = it.version,
                     source = it.source
                 )
             })
        }
    }

    fun updateRemoteSpecs(projectId: String, repoName: String) {
        val updateKey= "$LOCK_PREFIX:update_specs:$projectId:$repoName"
        redisOperation.get(updateKey)?.let {
            throw CocoapodsCommonException(CocoapodsMessageCode.COCOAPODS_INDEX_UPDATING_ERROR)
        }
        val repoInfo = repositoryClient.getRepoInfo(projectId, repoName).data?: throw RepoNotFoundException(repoName)
        if (repoInfo.category != RepositoryCategory.REMOTE || repoInfo.type != RepositoryType.COCOAPODS) {
            throw CocoapodsCommonException(CocoapodsMessageCode.COCOAPODS_INDEX_UPDATING_ERROR)
        }
        logger.info("project [$projectId], repo [$repoName],update remote specs...")
        redisOperation.set(key = updateKey, value = "1", expired = false)
        try {
            initRemoteSpecs(projectId, repoInfo)
        } finally {
            redisOperation.delete(updateKey)
        }
    }

    private fun cloneAndGetPodSpecs(conf: RemoteConfiguration, projectId: String, repoInfo: RepositoryInfo, tempPath: File, outputStream: ByteArrayOutputStream): MutableList<ArchiveModifier.Podspec> {
        val gitInstance = cocoapodsGitInstanceDao.findByUrl(conf.url)
        val specsGitPath = gitInstance?.path
            ?: PathUtil.buildSpecsGitPath(cocoapodsProperties.gitPath, projectId, repoInfo.name)
        val lockKey = "$LOCK_PREFIX:git_clone:$specsGitPath"
        lockOperation.doWithLock(lockKey) {
            val latestRef = GitUtil.cloneOrPullRepo(conf.url, specsGitPath)
            cocoapodsGitInstanceDao.saveIfNotExist(TCocoapodsGitInstance(url = conf.url, path = specsGitPath, ref = latestRef))
        }
        FileUtil.copyDirectory(specsGitPath, tempPath)
        return ArchiveModifier.modifyAndZip(tempPath, projectId, repoInfo.name, cocoapodsProperties.domain, outputStream)
    }

    private fun downloadAndGetPodSpecs(conf: RemoteConfiguration, cocoapodsConf: CocoapodsRemoteConfiguration, repoInfo: RepositoryInfo, projectId: String, outputStream: ByteArrayOutputStream, tempPath: File): MutableList<ArchiveModifier.Podspec> {
        val httpClient = buildOkHttpClient(conf).build()
        val url = PathUtil.buildRemoteSpecsUrl(cocoapodsConf, conf)
            ?: kotlin.run {
                logger.error("downloadUrl is null")
                throw CocoapodsCommonException(CocoapodsMessageCode.COCOAPODS_PODSPEC_NOT_FOUND)
            }
        val request = Request.Builder().url(url).build()
        logger.info("repo [${repoInfo.name}] Request url: $url, network config: ${conf.network}")
        val response = try {
            httpClient.newCall(request).execute()
        } catch (e: Exception) {
            logger.error("An error occurred while sending request $url", e)
            throw e
        }

        if (!response.isSuccessful) {
            logger.error("Request failed with status code")
        }
        response.body()?.byteStream()?.use { ips ->
            val wrap = ByteArrayInputStream(ips.readBytes())
            val fileType = FileUtil.detectFileType(wrap)
            ips.reset()
            return ArchiveModifier.modifyArchive(projectId, repoInfo.name, cocoapodsProperties.domain, wrap, outputStream, fileType, tempPath)
        } ?: run {
            logger.error("Failed to read the response body")
            throw CocoapodsCommonException(CocoapodsMessageCode.COCOAPODS_PODSPEC_NOT_FOUND)
        }
    }

    fun downloadSpecs(projectId: String, repoName: String) {
        val repoDetail = repositoryClient.getRepoDetail(projectId, repoName).data
            ?: throw RepoNotFoundException(repoName)
        val resource = when (repoDetail.category) {
            RepositoryCategory.LOCAL -> {
                //下载index文件,将.specs目录下的文件压缩返回
                val prefix = SLASH + DOT_SPECS
                val nodes = queryNodeDetailList(
                    projectId = projectId,
                    repoName = repoName,
                    prefix = prefix
                )
                val nodeMap = nodes.filterNot { it.folder }.associate {
                    val name = it.fullPath.removePrefix(prefix).ensurePrefix(com.tencent.bkrepo.cocoapods.constant.SPECS)
                    name to run {
                        nodeClient.updateRecentlyUseDate(it.projectId, it.repoName, it.fullPath)
                        storageManager.loadArtifactInputStream(it, repoDetail.storageCredentials)
                            ?: throw ArtifactNotFoundException(it.fullPath)
                    }
                }
                if (nodeMap.isEmpty()) {
                    returnEmptySpec()
                }
                ArtifactResource(
                    artifactMap = nodeMap,
                    srcRepo = RepositoryIdentify(projectId, repoName),
                    useDisposition = true,
                    contentType = APPLICATION_GZIP
                )
            }

            RepositoryCategory.REMOTE -> {
                //下载index文件
                if (indexExist(projectId, repoName).not()) {
                    logger.warn("repo $repoName index file not exist")
                    val repoInfo = repositoryClient.getRepoInfo(projectId, repoName).data
                        ?: throw RepoNotFoundException(repoName)
                    initRemoteSpecs(projectId, repoInfo)
                }
                val node = nodeClient.getNodeDetail(projectId, repoName, generateIndexTarPath()).data
                    ?: throw NodeNotFoundException(generateIndexTarPath())
                val inputStream = storageManager.loadArtifactInputStream(node, repoDetail.storageCredentials)
                    ?: throw ArtifactNotFoundException(generateIndexTarPath())
                ArtifactResource(
                    inputStream = inputStream,
                    artifactName = generateIndexTarPath(),
                    srcRepo = RepositoryIdentify(projectId, repoName),
                    contentType = APPLICATION_ZIP
                )
            }

            else -> throw RepoNotFoundException(repoName)
        }
        artifactResourceWriterContext.getWriter(resource).write(resource)
    }

    fun indexExist(projectId: String, repoName: String): Boolean {
        return nodeClient.checkExist(projectId, repoName, generateIndexTarPath()).data ?: false
    }

    private fun queryNodeDetailList(
        projectId: String,
        repoName: String,
        prefix: String,
    ): List<NodeDetail> {
        var pageNumber = 1
        val nodeDetailList = mutableListOf<NodeDetail>()
        val count = nodeClient.countFileNode(projectId, repoName, prefix).data ?: 0
        do {
            val option = NodeListOption(
                pageNumber = pageNumber,
                pageSize = 1000,
                includeFolder = true,
                includeMetadata = true,
                deep = true
            )
            val records = nodeClient.listNodePage(projectId, repoName, prefix, option).data?.records
            if (records.isNullOrEmpty()) {
                break
            }
            nodeDetailList.addAll(
                records.map { NodeDetail(it) }
            )
            pageNumber++
        } while (nodeDetailList.size < count)
        return nodeDetailList
    }

    private fun returnEmptySpec() {
        val response = getResponse()

        try {
            response.contentType = APPLICATION_GZIP
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, encodeDisposition("response.gz"))
            val gzipOutputStream = buildEmptySpecGzOps(response)
            gzipOutputStream.finish()
        } catch (e: IOException) {
            logger.error("Error occurred while creating archive.", e)
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create archive.")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CocoapodsWebService::class.java)
    }
}

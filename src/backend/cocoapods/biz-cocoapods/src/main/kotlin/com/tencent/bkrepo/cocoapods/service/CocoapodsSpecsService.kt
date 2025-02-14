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
import com.tencent.bkrepo.cocoapods.model.TCocoapodsGitInstance
import com.tencent.bkrepo.cocoapods.model.TCocoapodsRemotePackage
import com.tencent.bkrepo.cocoapods.pojo.enums.RemoteRepoType
import com.tencent.bkrepo.cocoapods.utils.FileUtil
import com.tencent.bkrepo.cocoapods.utils.GitUtil
import com.tencent.bkrepo.cocoapods.utils.ObjectBuildUtil.toCocoapodsRemoteConfiguration
import com.tencent.bkrepo.cocoapods.utils.PathUtil
import com.tencent.bkrepo.cocoapods.utils.PathUtil.generateIndexTarPath
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.artifact.exception.RepoNotFoundException
import com.tencent.bkrepo.common.artifact.manager.StorageManager
import com.tencent.bkrepo.common.artifact.pojo.configuration.remote.RemoteConfiguration
import com.tencent.bkrepo.common.artifact.repository.remote.buildOkHttpClient
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.lock.service.LockOperation
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepositoryInfo
import okhttp3.Request
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

@Service
class CocoapodsSpecsService(
    private val nodeClient: NodeClient,
    private val repositoryClient: RepositoryClient,
    private val storageManager: StorageManager,
    private val cocoapodsProperties: CocoapodsProperties,
    private val lockOperation: LockOperation,
    private val cocoapodsRemotePackageDao: CocoapodsRemotePackageDao,
    private val cocoapodsGitInstanceDao: CocoapodsGitInstanceDao,
    private val cocoapodsRepoService: CocoapodsRepoService,
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
        cocoapodsRepoService.updateStringSetting(projectId, repoInfo.name, INDEX_GENERATING, INDEX_GENERATING_VALUE_DOING)
        val url = PathUtil.buildRemoteSpecsUrl(cocoapodsConf, conf)
            ?: kotlin.run {
                logger.error("downloadUrl is null")
                throw ErrorCodeException(CocoapodsMessageCode.COCOAPODS_PODSPEC_NOT_FOUND)
            }
        try {
            val podspecList = when (cocoapodsConf.type) {
                RemoteRepoType.GIT_HUB -> {
                    cloneAndGetPodSpecs(conf, url, projectId, repoInfo, tempPath, outputStream)
                }

                else -> {
                    downloadAndGetPodSpecs(conf, url, repoInfo, projectId, outputStream, tempPath)
                }
            }
            if (podspecList.isEmpty()) {
                logger.warn("project [$projectId], repo [${repoInfo.name}], no podspec found")
                return
            }
            val repoDetail = repositoryClient.getRepoDetail(projectId, repoInfo.name).data
                ?: throw RepoNotFoundException(repoInfo.name)

            val specArtifact =
                ArtifactFileFactory.build(outputStream.toByteArray().inputStream(), repoDetail.storageCredentials)
            val nodeCreateRequest = NodeCreateRequest(
                projectId = projectId,
                repoName = repoInfo.name,
                fullPath = generateIndexTarPath(),
                folder = false,
                size = specArtifact.getSize(),
                sha256 = specArtifact.getFileSha256(),
                md5 = specArtifact.getFileMd5(),
                overwrite = true,
                operator = SecurityUtils.getUserId()
            )
            storageManager.storeArtifactFile(nodeCreateRequest, specArtifact, null)
            cocoapodsRemotePackageDao.saveIfNotExists(podspecList.map {
                TCocoapodsRemotePackage(
                    projectId = projectId,
                    repoName = repoInfo.name,
                    packageName = it.name,
                    packageVersion = it.version,
                    source = it.source
                )
            })
        } finally {
            cocoapodsRepoService.updateStringSetting(projectId, repoInfo.name, INDEX_GENERATING, INDEX_GENERATING_VALUE_DONE)
        }
        logger.info("project [$projectId], repo [${repoInfo.name},url:$url],init remote specs success")
    }

    private fun cloneAndGetPodSpecs(conf: RemoteConfiguration,remoteUrl: String, projectId: String, repoInfo: RepositoryInfo, tempPath: File, outputStream: ByteArrayOutputStream): MutableList<ArchiveModifier.Podspec> {
        var credentialsProvider: CredentialsProvider? = null
        if (conf.credentials.username != null &&
            conf.credentials.password != null
        ) {
            credentialsProvider = UsernamePasswordCredentialsProvider(
                conf.credentials.username, conf.credentials.password
            )
        }
        val gitInstance = cocoapodsGitInstanceDao.findByUrl(remoteUrl)
        val specsGitPath = gitInstance?.path
            ?: PathUtil.buildSpecsGitPath(cocoapodsProperties.gitPath, projectId, repoInfo.name)
        val lockKey = "$LOCK_PREFIX:git_clone:$specsGitPath"
        val oldLatestRef = cocoapodsRepoService.getStringSetting(projectId, repoInfo.name, LATEST_REF)
        var latestRef = ""
        lockOperation.doWithLock(lockKey) {
            latestRef = GitUtil.cloneOrPullRepo(remoteUrl, specsGitPath, credentialsProvider)
            cocoapodsGitInstanceDao.saveIfNotExist(TCocoapodsGitInstance(url = remoteUrl, path = specsGitPath, ref = latestRef))
        }
        logger.info("repo [${repoInfo.name}] latestRef: $latestRef , oldLatestRef: $oldLatestRef")
        //如果当前已是最新的引用，则不需要再更新
        if (oldLatestRef == latestRef && indexExist(projectId, repoInfo.name)) {
            logger.info("repo [${repoInfo.name}] is latest, no need to update")
            return mutableListOf()
        }
        FileUtil.copyDirectory(specsGitPath, tempPath)
        cocoapodsRepoService.updateStringSetting(projectId, repoInfo.name, LATEST_REF, latestRef)
        return ArchiveModifier.modifyAndZip(tempPath, projectId, repoInfo.name, cocoapodsProperties.domain, outputStream)
    }

    private fun downloadAndGetPodSpecs(conf: RemoteConfiguration, remoteUrl: String, repoInfo: RepositoryInfo, projectId: String, outputStream: ByteArrayOutputStream, tempPath: File): MutableList<ArchiveModifier.Podspec> {
        val httpClient = buildOkHttpClient(conf).build()

        val request = Request.Builder().url(remoteUrl).build()
        logger.info("repo [${repoInfo.name}] Request url: $remoteUrl, network config: ${conf.network}")
        val response = try {
            httpClient.newCall(request).execute()
        } catch (e: Exception) {
            logger.error("An error occurred while sending request $remoteUrl", e)
            throw e
        }

        if (!response.isSuccessful) {
            logger.error("Request failed with status code, url: $remoteUrl, message: ${response.message()}")
            throw ErrorCodeException(CocoapodsMessageCode.COCOAPODS_PODSPEC_NOT_FOUND)
        }
        response.body()?.byteStream()?.use { ips ->
            val wrap = ByteArrayInputStream(ips.readBytes())
            val fileType = FileUtil.detectFileType(wrap)
            wrap.reset()
            return ArchiveModifier.modifyArchive(projectId, repoInfo.name, cocoapodsProperties.domain, wrap, outputStream, fileType, tempPath)
        } ?: run {
            logger.error("Failed to read the response body")
            throw ErrorCodeException(CocoapodsMessageCode.COCOAPODS_PODSPEC_NOT_FOUND)
        }
    }

    fun indexExist(projectId: String, repoName: String): Boolean {
        return nodeClient.checkExist(projectId, repoName, generateIndexTarPath()).data ?: false
    }

    companion object {
        const val LATEST_REF = "latestRef"
        const val INDEX_GENERATING = "indexGenerating"
        const val INDEX_GENERATING_VALUE_DOING = "doing"
        const val INDEX_GENERATING_VALUE_DONE = "done"
        private val logger = LoggerFactory.getLogger(CocoapodsWebService::class.java)
    }
}

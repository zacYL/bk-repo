/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.npm.artifact.repository

import com.github.zafarkhaja.semver.Version
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.util.JsonUtils
import com.tencent.bkrepo.common.api.util.UrlFormatter
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.exception.NodeNotFoundException
import com.tencent.bkrepo.common.artifact.hash.sha1
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.pojo.RepositoryId
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactMigrateContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactSearchContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.local.LocalRepository
import com.tencent.bkrepo.common.artifact.repository.migration.MigrateDetail
import com.tencent.bkrepo.common.artifact.repository.migration.PackageMigrateDetail
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.artifact.stream.ArtifactInputStream
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo
import com.tencent.bkrepo.npm.constants.ATTRIBUTE_OCTET_STREAM_SHA1
import com.tencent.bkrepo.npm.constants.CREATED
import com.tencent.bkrepo.npm.constants.HAR_FILE_EXT
import com.tencent.bkrepo.npm.constants.HSP_FILE_EXT
import com.tencent.bkrepo.npm.constants.LATEST
import com.tencent.bkrepo.npm.constants.METADATA
import com.tencent.bkrepo.npm.constants.MODIFIED
import com.tencent.bkrepo.npm.constants.NPM_FILE_FULL_PATH
import com.tencent.bkrepo.npm.constants.OHPM_CHANGELOG_FILE_NAME
import com.tencent.bkrepo.npm.constants.OHPM_README_FILE_NAME
import com.tencent.bkrepo.npm.constants.RESOLVED_HSP
import com.tencent.bkrepo.npm.constants.SEARCH_REQUEST
import com.tencent.bkrepo.npm.constants.SIZE
import com.tencent.bkrepo.npm.constants.TARBALL_FULL_PATH
import com.tencent.bkrepo.npm.handler.NpmDependentHandler
import com.tencent.bkrepo.npm.model.metadata.NpmPackageMetaData
import com.tencent.bkrepo.npm.model.metadata.NpmVersionMetadata
import com.tencent.bkrepo.npm.pojo.NpmSearchInfo
import com.tencent.bkrepo.npm.pojo.NpmSearchInfoMap
import com.tencent.bkrepo.npm.pojo.enums.NpmOperationAction
import com.tencent.bkrepo.npm.pojo.metadata.MetadataSearchRequest
import com.tencent.bkrepo.npm.properties.NpmProperties
import com.tencent.bkrepo.npm.service.NpmOperationService
import com.tencent.bkrepo.npm.utils.NpmUtils
import com.tencent.bkrepo.npm.utils.OkHttpUtil
import com.tencent.bkrepo.npm.utils.TimeUtil
import com.tencent.bkrepo.repository.pojo.download.PackageDownloadRecord
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodesDeleteRequest
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import com.tencent.bkrepo.repository.pojo.search.NodeQueryBuilder
import okhttp3.Response
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.IOException
import java.io.InputStream
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.system.measureTimeMillis

@Component
class NpmLocalRepository(
    private val npmProperties: NpmProperties,
    private val okHttpUtil: OkHttpUtil,
    private val npmDependentHandler: NpmDependentHandler,
    private val npmOperationService: NpmOperationService
) : LocalRepository() {

    override fun onUploadBefore(context: ArtifactUploadContext) {
        super.onUploadBefore(context)
        // 不为空说明上传的是tgz文件
        context.getStringAttribute("attachments.content_type")?.let {
            // TODO: 需要抽象处理
            packageVersion(context, null)?.let { uploadIntercept(context, it) }
            // 计算sha1并校验
            val calculatedSha1 = context.getArtifactFile().getInputStream().sha1()
            val uploadSha1 = context.getStringAttribute(ATTRIBUTE_OCTET_STREAM_SHA1)
            if (uploadSha1 != null && calculatedSha1 != uploadSha1) {
                throw ErrorCodeException(ArtifactMessageCode.DIGEST_CHECK_FAILED, "sha1")
            }
        }
    }
    override fun onDownloadBefore(context: ArtifactDownloadContext) {
        super.onDownloadBefore(context)
        downloadIntercept(context, null)
    }

    override fun buildNodeCreateRequest(context: ArtifactUploadContext): NodeCreateRequest {
        return NodeCreateRequest(
            projectId = context.projectId,
            repoName = context.repoName,
            folder = false,
            fullPath = context.getStringAttribute(NPM_FILE_FULL_PATH)!!,
            size = context.getArtifactFile().getSize(),
            sha256 = context.getArtifactSha256(),
            md5 = context.getArtifactMd5(),
            operator = context.userId,
            overwrite = true
        )
    }

    override fun query(context: ArtifactQueryContext): ArtifactInputStream? {
        val fullPath = context.getStringAttribute(NPM_FILE_FULL_PATH)!!
        if (ArtifactContextHolder.getUrlPath(this::javaClass.name)?.startsWith("/ext/") != true) {
            context.getFullPathInterceptors().forEach { it.intercept(context.projectId, fullPath) }
        }
        return this.onQuery(context) ?: run {
            logger.warn("Artifact [$fullPath] not found in repo [${context.projectId}/${context.repoName}]")
            null
        }
    }

    private fun onQuery(context: ArtifactQueryContext): ArtifactInputStream? {
        val repositoryDetail = context.repositoryDetail
        val projectId = repositoryDetail.projectId
        val repoName = repositoryDetail.name
        val fullPath = context.getStringAttribute(NPM_FILE_FULL_PATH)!!
        val node = nodeService.getNodeDetail(ArtifactInfo(projectId, repoName, fullPath))
        if (node == null || node.folder) return null
        return storageManager.loadFullArtifactInputStream(node, context.storageCredentials)
            .also {
                logger.info("search artifact [$fullPath] success in repo [${context.artifactInfo.getRepoIdentify()}]")
            }
    }

    override fun buildDownloadRecord(
        context: ArtifactDownloadContext,
        artifactResource: ArtifactResource
    ): PackageDownloadRecord? {
        with(context) {
            if (artifactInfo.getArtifactFullPath().endsWith(HSP_FILE_EXT)) {
                // 拉取package一次会同时下载har与hsp包，此处仅统计har，避免重复
                return null
            }
            val packageInfo = NpmUtils.parseNameAndVersionFromFullPath(artifactInfo.getArtifactFullPath())
            with(packageInfo) {
                val packageKey = NpmUtils.packageKeyByRepoType(first, repositoryDetail.type)
                return PackageDownloadRecord(projectId, repoName, packageKey, second, userId)
            }
        }
    }

    override fun search(context: ArtifactSearchContext): List<NpmSearchInfoMap> {
        val searchRequest = context.getAttribute<MetadataSearchRequest>(SEARCH_REQUEST)!!

        val queryModel = NodeQueryBuilder()
            .select("projectId", "repoName", "fullPath", "metadata", "lastModifiedDate")
            .sortByDesc("lastModifiedDate")
            .page(searchRequest.from, searchRequest.size)
            .projectId(context.projectId).repoName(context.repoName)
            .fullPath(".tgz", OperationType.SUFFIX)
            .or()
            .metadata("name", searchRequest.text, OperationType.MATCH)
            .metadata("description", searchRequest.text, OperationType.MATCH)
            .metadata("maintainers", searchRequest.text, OperationType.MATCH)
            .metadata("version", searchRequest.text, OperationType.MATCH)
            .metadata("keywords", searchRequest.text, OperationType.MATCH)
            .build()
        val data = nodeSearchService.searchWithoutCount(queryModel)
        return transferRecords(data.records)
    }

    @Suppress("UNCHECKED_CAST")
    private fun transferRecords(records: List<Map<String, Any?>>): List<NpmSearchInfoMap> {
        val mapListInfo = mutableListOf<NpmSearchInfoMap>()
        if (records.isNullOrEmpty()) return emptyList()
        records.forEach {
            val metadata = it[METADATA] as Map<String, Any>
            mapListInfo.add(
                NpmSearchInfoMap(
                    NpmSearchInfo(
                        metadata["name"] as? String,
                        metadata["description"] as? String,
                        metadata["maintainers"] as? List<Map<String, Any>> ?: emptyList(),
                        metadata["version"] as? String,
                        (it["lastModifiedDate"] as LocalDateTime).toString(),
                        metadata["keywords"] as? List<String> ?: emptyList(),
                        metadata["author"] as? Map<String, Any> ?: emptyMap()
                    )
                )
            )
        }
        return mapListInfo
    }

    override fun remove(context: ArtifactRemoveContext) {
        with(context) {
            val npmArtifactInfo = artifactInfo as NpmArtifactInfo
            val tarballPath = getStringAttribute(TARBALL_FULL_PATH)
            val fullPaths = NpmUtils.getContentPathsToDelete(
                npmArtifactInfo, tarballPath, context.repositoryDetail.type
            )
            nodeService.deleteNodes(NodesDeleteRequest(projectId, repoName, fullPaths, userId))
            logger.info("delete artifact $fullPaths success in repo [$projectId/$repoName].")
        }
    }

    override fun packageVersion(context: ArtifactContext?, node: NodeDetail?): PackageVersion? {
        requireNotNull(context)
        return npmOperationService.packageVersion(context)
    }

    /**
     *  迁移成功之后需要创建包
     *  迁移tgz包时需要创建node元数据
     */
    override fun migrate(context: ArtifactMigrateContext): MigrateDetail {
        val dataSet = context.getAttribute<Set<String>>("migrationDateSet")!!
        val packageList: MutableList<PackageMigrateDetail> = mutableListOf()
        with(context) {
            val iterator = dataSet.iterator()
            val millis = measureTimeMillis {
                while (iterator.hasNext()) {
                    val packageMigrateDetail = doMigrate(context, iterator.next())
                    packageList.add(packageMigrateDetail)
                }
            }
            return MigrateDetail(
                projectId,
                repoName,
                packageList,
                Duration.ofMillis(millis),
                "npm package migrate result"
            )
        }
    }

    override fun getArtifactFullPaths(
        projectId: String,
        repoName: String,
        key: String,
        version: String,
        manifestPath: String?,
        artifactPath: String?
    ): List<String> {
        require(artifactPath != null)
        val repoType = ArtifactContextHolder.getRepoDetail(RepositoryId(projectId, repoName)).type
        val name = NpmUtils.resolveNameByRepoType(key, repoType)
        val versionMetadataFullPath = NpmUtils.getVersionPackageMetadataPath(name, version)
        if (nodeService.checkExist(ArtifactInfo(projectId, repoName, versionMetadataFullPath)) != true) {
            throw NodeNotFoundException("$projectId/$repoName/$versionMetadataFullPath")
        }
        val fullPaths = mutableListOf(artifactPath, versionMetadataFullPath)
        if (artifactPath.endsWith(HAR_FILE_EXT)) {
            val hspFullPath = artifactPath.removeSuffix(HAR_FILE_EXT) + HSP_FILE_EXT
            if (nodeClient.checkExist(projectId, repoName, hspFullPath).data == true) fullPaths.add(hspFullPath)
        }
        return fullPaths
    }

    /**
     * TODO: 需要处理并发写入
     */
    override fun updateIndex(projectId: String, repoName: String, key: String, version: String) {
        logger.info("update index for $projectId/$repoName/$key/$version")
        val repo = ArtifactContextHolder.getRepoDetail(RepositoryId(projectId, repoName))
        val name = NpmUtils.resolveNameByRepoType(key, repo.type)
        val pkgMetadataFullPath = NpmUtils.getPackageMetadataPath(name)
        val credential = repo.storageCredentials

        // 获取版本索引
        val verMetadataFullPath = NpmUtils.getVersionPackageMetadataPath(name, version)
        val verMetadataNode = nodeService.getNodeDetail(ArtifactInfo(projectId, repoName, verMetadataFullPath))
            ?: throw NodeNotFoundException("$projectId/$repoName/$verMetadataFullPath")
        val versionMetadata = storageManager.loadArtifactInputStream(verMetadataNode, credential).use {
            JsonUtils.objectMapper.readValue(it, NpmVersionMetadata::class.java)
        }
        // 获取目标仓库已有的package索引
        val packageExist = packageService.findPackageByKey(projectId, repoName, key) != null
        val originalMetadata = if (packageExist) {
            val originMetadataNode = nodeService.getNodeDetail(ArtifactInfo(projectId, repoName, pkgMetadataFullPath))
                ?: throw NodeNotFoundException("$projectId/$repoName/$pkgMetadataFullPath")
            storageManager.loadArtifactInputStream(originMetadataNode, credential).use {
                JsonUtils.objectMapper.readValue(it, NpmPackageMetaData::class.java)
            }
        } else null
        // 构建新的package索引
        val artifactFile = buildNewMetadataFile(projectId, repoName, versionMetadata, originalMetadata)
        val nodeCreateRequest = NodeCreateRequest(
            projectId = projectId,
            repoName = repoName,
            fullPath = pkgMetadataFullPath,
            folder = false,
            size = artifactFile.getSize(),
            sha256 = artifactFile.getFileSha256(),
            md5 = artifactFile.getFileMd5(),
            overwrite = true,
            operator = SecurityUtils.getPrincipal()
        )
        storageManager.storeArtifactFile(nodeCreateRequest, artifactFile, credential)
        logger.info("success to update index for [$projectId/$repoName/$name/$version]")
        super.updateIndex(projectId, repoName, key, version)
    }

    private fun buildNewMetadataFile(
        projectId: String,
        repoName: String,
        versionMetadata: NpmVersionMetadata,
        originalMetadata: NpmPackageMetaData?
    ): ArtifactFile {
        val name = versionMetadata.name!!
        val version = versionMetadata.version!!
        val current = TimeUtil.getGMTTime()
        val packageMetadata = if (originalMetadata == null) {
            NpmPackageMetaData().apply {
                this.id = name
                this.name = name
                this.distTags = NpmPackageMetaData.DistTags().apply { set(LATEST, version) }
                this.versions.map = mutableMapOf(version to versionMetadata)
                this.time = NpmPackageMetaData.Time().apply {
                    add(CREATED, current)
                    add(MODIFIED, current)
                    add(version, current)
                }
            }
        } else {
            originalMetadata.versions.map[version] = versionMetadata
            originalMetadata.time.apply {
                add(MODIFIED, current)
                add(version, current)
            }
            val latest = originalMetadata.distTags.getMap()[LATEST]
            if (latest == null || latest != version && Version.valueOf(latest) < Version.valueOf(version)) {
                originalMetadata.distTags.set(LATEST, version)
            }
            originalMetadata
        }
        val oldTarball = versionMetadata.dist?.tarball!!
        val dist = packageMetadata.versions.map[version]!!.dist
        dist?.tarball =
            NpmUtils.buildPackageTgzTarball(
                oldTarball, npmProperties.domain, npmProperties.tarball.prefix, npmProperties.returnRepoId, name,
                NpmArtifactInfo(projectId, repoName, name, version)
            )
        if (StringUtils.isNotBlank(dist?.resolvedHsp)) {
            dist?.resolvedHsp = dist?.tarball?.substringBeforeLast(".") + HSP_FILE_EXT
        }
        val credential = ArtifactContextHolder.getRepoDetail(RepositoryId(projectId, repoName)).storageCredentials
        return packageMetadata.toJsonString().byteInputStream().let { ArtifactFileFactory.build(it, credential) }
    }

    private fun doMigrate(context: ArtifactMigrateContext, packageName: String): PackageMigrateDetail {
        val packageMetaData = queryRemotePackageMetadata(packageName)
        return migratePackageArtifact(context, packageMetaData)
    }

    private fun queryRemotePackageMetadata(packageName: String): NpmPackageMetaData {
        val url = UrlFormatter.format(npmProperties.migration.remoteRegistry, packageName)
        var response: Response? = null
        try {
            response = okHttpUtil.doGet(url)
            return response.body!!.byteStream()
                .use { JsonUtils.objectMapper.readValue(it, NpmPackageMetaData::class.java) }
        } catch (exception: IOException) {
            logger.error(
                "migrate: http send [$url] for search [$packageName/package.json] file failed, {}",
                exception
            )
            throw exception
        } finally {
            response?.body?.close()
        }
    }

    private fun migratePackageArtifact(
        context: ArtifactMigrateContext,
        packageMetaData: NpmPackageMetaData
    ): PackageMigrateDetail {
        val versionSizeMap = mutableMapOf<String, Long>()
        val name = packageMetaData.name!!
        val packageMigrateDetail = PackageMigrateDetail(name)
        var count = 0
        val totalSize = packageMetaData.versions.map.size
        val iterator = packageMetaData.versions.map.entries.iterator()
        val ohpm = context.repositoryDetail.type == RepositoryType.OHPM
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val version = entry.key
            val versionMetadata = entry.value
            try {
                measureTimeMillis {
                    val tarball = versionMetadata.dist?.tarball!!
                    storeVersionMetadata(context, versionMetadata)
                    var size = storeTgzArtifact(context, tarball, name, version, NpmUtils.getContentFileExt(ohpm))
                    versionMetadata.dist?.any()?.get(RESOLVED_HSP)?.let {
                        // ohpm包存储hsp文件
                        size += storeTgzArtifact(context, it as String, name, version, HSP_FILE_EXT)
                    }
                    versionSizeMap[version] = size
                }.apply {
                    logger.info(
                        "migrate npm package [$name] for version [$version] success, elapse $this ms. " +
                                "process rate: [${++count}/$totalSize]"
                    )
                }
                packageMigrateDetail.addSuccessVersion(version)
            } catch (ignored: Exception) {
                logger.error("migrate package [$name] for version [$version] failed， message： $ignored")
                // delete version metadata
                deleteVersionMetadata(context, name, version)
                packageMigrateDetail.addFailureVersion(version, ignored.toString())
            }
        }
        val failVersionList = packageMigrateDetail.failureVersionDetailList.map { it.version }
        migratePackageMetadata(context, packageMetaData, failVersionList, versionSizeMap)
        return packageMigrateDetail
    }

    /**
     * 迁移package.json文件
     */
    private fun migratePackageMetadata(
        context: ArtifactMigrateContext,
        packageMetaData: NpmPackageMetaData,
        failVersionList: List<String>,
        versionSizeMap: Map<String, Long>
    ) {
        val name = packageMetaData.name!!
        val fullPath = NpmUtils.getPackageMetadataPath(name)
        val ohpm = context.repositoryDetail.type == RepositoryType.OHPM
        try {
            with(context) {
                val originalPackageMetadata = npmPackageMetaData(fullPath)
                val newPackageMetaData = if (originalPackageMetadata != null) {
                    // 比对合并package.json，将失败的版本去除
                    comparePackageVersion(originalPackageMetadata, packageMetaData, failVersionList, versionSizeMap)
                } else {
                    // 本地没有该文件直接迁移，将失败的版本去除
                    migratePackageVersion(packageMetaData, failVersionList, versionSizeMap)
                } ?: return
                // 调整tarball地址
                adjustTarball(newPackageMetaData, name, packageMetaData)
                if (ohpm) {
                    newPackageMetaData.rev = newPackageMetaData.versions.map.size.toString()
                }
                // 存储package.json文件
                context.putAttribute(NPM_FILE_FULL_PATH, NpmUtils.getPackageMetadataPath(name))
                val artifactFile = JsonUtils.objectMapper.writeValueAsBytes(newPackageMetaData).inputStream()
                    .use { ArtifactFileFactory.build(it) }
                val nodeCreateRequest = buildMigrationNodeCreateRequest(context, artifactFile)
                storageManager.storeArtifactFile(nodeCreateRequest, artifactFile, storageCredentials)
                // 添加依赖
                npmDependentHandler.updatePackageDependents(
                    context.userId, context.artifactInfo, newPackageMetaData, NpmOperationAction.MIGRATION, ohpm
                )
                artifactFile.delete()
            }
        } catch (ignored: Exception) {
            logger.error("migrate package metadata for package [$name] failed. message: ${ignored.message}")
        }
    }

    private fun adjustTarball(
        newPackageMetaData: NpmPackageMetaData,
        name: String,
        packageMetaData: NpmPackageMetaData
    ) {
        val versionMetaData = newPackageMetaData.versions.map.values.iterator().next()
        with(versionMetaData) {
            if (!NpmUtils.isDashSeparateInTarball(name, version!!, dist?.tarball!!)) {
                packageMetaData.versions.map.values.forEach {
                    adjustTarball(it)
                }
            }
        }
    }

    private fun ArtifactMigrateContext.npmPackageMetaData(fullPath: String): NpmPackageMetaData? {
        val node = nodeService.getNodeDetail(ArtifactInfo(projectId, repoName, fullPath))
        return node?.let {
            val inputStream = storageManager.loadFullArtifactInputStream(it, storageCredentials)
            JsonUtils.objectMapper.readValue(inputStream, NpmPackageMetaData::class.java)
        }
    }

    private fun comparePackageVersion(
        originalPackageMetadata: NpmPackageMetaData,
        packageMetaData: NpmPackageMetaData,
        failVersionList: List<String>,
        versionSizeMap: Map<String, Long>
    ): NpmPackageMetaData {
        addPackageSizeField(originalPackageMetadata, versionSizeMap)
        val name = originalPackageMetadata.name.orEmpty()
        val originalVersionSet = originalPackageMetadata.versions.map.keys
        val remoteVersionList = packageMetaData.versions.map.keys
        remoteVersionList.removeAll(originalVersionSet)
        remoteVersionList.removeAll(failVersionList)
        val originalDistTags = originalPackageMetadata.distTags
        val originalTimeMap = originalPackageMetadata.time
        val distTags = packageMetaData.distTags
        val timeMap = packageMetaData.time
        // 迁移dist_tags
        migrateDistTags(distTags, originalDistTags)
        // 迁移latest版本
        migrateLatest(distTags, originalDistTags, timeMap, originalTimeMap)
        logger.info(
            "the different versions of the  package [$name] is [$remoteVersionList], " +
                    "size : ${remoteVersionList.size}"
        )
        if (remoteVersionList.isEmpty()) return originalPackageMetadata
        // 说明有版本更新，将新增的版本迁移过来
        remoteVersionList.forEach { it ->
            val versionMetadata = packageMetaData.versions.map[it]!!
            val versionTime = timeMap.get(it)
            // 比较两边都存在相同的版本，则比较上传时间, 如果remote版本在后面上传，则进行迁移
            if (originalVersionSet.contains(it)) {
                if (TimeUtil.compareTime(versionTime, originalTimeMap.get(it))) {
                    originalPackageMetadata.versions.map[it] = versionMetadata
                    originalTimeMap.add(it, versionTime)
                    migrateDistTagsToOriginal(distTags, originalDistTags)
                }
            } else {
                originalPackageMetadata.versions.map[it] = versionMetadata
                originalTimeMap.add(it, versionTime)
            }
        }
        return originalPackageMetadata
    }

    private fun migrateDistTagsToOriginal(
        distTags: NpmPackageMetaData.DistTags,
        originalDistTags: NpmPackageMetaData.DistTags
    ) {
        distTags.getMap().entries.forEach {
            originalDistTags.set(it.key, it.value)
        }
    }

    private fun migrateDistTags(distTags: NpmPackageMetaData.DistTags, originalDistTags: NpmPackageMetaData.DistTags) {
        distTags.getMap().entries.forEach {
            if (!originalDistTags.getMap().keys.contains(it.key)) {
                originalDistTags.set(it.key, it.value)
            }
        }
    }

    private fun migrateLatest(
        distTags: NpmPackageMetaData.DistTags,
        originalDistTags: NpmPackageMetaData.DistTags,
        timeMap: NpmPackageMetaData.Time,
        originalTimeMap: NpmPackageMetaData.Time
    ) {
        val remoteLatest = NpmUtils.getLatestVersionFormDistTags(distTags)
        val originalLatest = NpmUtils.getLatestVersionFormDistTags(originalDistTags)
        if (TimeUtil.compareTime(timeMap.get(remoteLatest), originalTimeMap.get(originalLatest))) {
            originalDistTags.set("latest", remoteLatest)
            originalTimeMap.add("modified", timeMap.get("modified"))
        }
    }

    /**
     * 迁移版本成功的包
     */
    private fun migratePackageVersion(
        packageMetaData: NpmPackageMetaData,
        failVersionList: List<String>,
        versionSizeMap: Map<String, Long>
    ): NpmPackageMetaData? {
        // dist对象添加size字段
        addPackageSizeField(packageMetaData, versionSizeMap)
        if (failVersionList.isEmpty()) return packageMetaData
        val remoteVersionList = packageMetaData.versions.map.keys
        remoteVersionList.removeAll(failVersionList)
        if (remoteVersionList.isEmpty()) return null

        val distTags = packageMetaData.distTags
        val timeMap = packageMetaData.time.getMap()
        val latest = NpmUtils.getLatestVersionFormDistTags(distTags)
        val iterator = distTags.getMap().entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (failVersionList.contains(entry.value)) {
                iterator.remove()
                timeMap.remove(entry.value)
            }
        }
        if (!timeMap.containsKey(latest)) {
            timeMap.remove("modified")
            val timeList = timeMap.entries.map { LocalDateTime.parse(it.value, DateTimeFormatter.ISO_DATE_TIME) }
            val maxTime = Collections.max(timeList)
            val latestTime = maxTime.format(DateTimeFormatter.ofPattern(TimeUtil.FORMAT))
            timeMap.entries.forEach {
                if (it.value == latestTime) {
                    distTags.set("latest", it.key)
                }
            }
            timeMap["modified"] = latestTime
        }
        return packageMetaData
    }

    private fun adjustTarball(versionMetaData: NpmVersionMetadata) {
        with(versionMetaData) {
            versionMetaData.dist!!.tarball = NpmUtils.formatTarballWithDash(name!!, version!!, dist?.tarball!!)
            versionMetaData.dist!!.any()[RESOLVED_HSP]?.let {
                val resolvedHsp = NpmUtils.formatTarballWithDash(name!!, version!!, it as String)
                versionMetaData.dist!!.set(RESOLVED_HSP, resolvedHsp)
            }
        }
    }

    private fun addPackageSizeField(packageMetaData: NpmPackageMetaData, versionSizeMap: Map<String, Long>) {
        val versionsMap = packageMetaData.versions.map
        versionsMap.entries.forEach {
            if (!versionsMap.containsKey(SIZE)) {
                it.value.dist?.set(SIZE, versionSizeMap[it.key])
            }
        }
    }

    private fun storeVersionMetadata(context: ArtifactMigrateContext, versionMetadata: NpmVersionMetadata) {
        with(context) {
            val name = versionMetadata.name!!
            val version = versionMetadata.version!!
            if (!NpmUtils.isDashSeparateInTarball(name, version, versionMetadata.dist?.tarball!!)) {
                adjustTarball(versionMetadata)
            }
            val inputStream = JsonUtils.objectMapper.writeValueAsString(versionMetadata).byteInputStream()
            val artifactFile = inputStream.use { ArtifactFileFactory.build(it) }
            val fullPath = NpmUtils.getVersionPackageMetadataPath(name, version)
            context.putAttribute(NPM_FILE_FULL_PATH, fullPath)
            if (nodeService.checkExist(ArtifactInfo(projectId, repoName, fullPath))) {
                logger.info(
                    "package [$name] with version metadata [$name-$version.json] " +
                            "is already exists in repository [$projectId/$repoName], skip migration."
                )
                return
            }
            val nodeCreateRequest = buildMigrationNodeCreateRequest(context, artifactFile)
            storageManager.storeArtifactFile(nodeCreateRequest, artifactFile, storageCredentials)
            logger.info("migrate npm package [$name] with version metadata [$name-$version.json] success.")
            artifactFile.delete()
        }
    }

    private fun storeTgzArtifact(
        context: ArtifactMigrateContext,
        tarball: String,
        name: String,
        version: String,
        ext: String,
    ): Long {
        // 包的大小信息
        with(context) {
            var size = 0L
            var response: Response? = null
            val fullPath = NpmUtils.getTarballFullPath(name, version, ext)
            // hit cache continue
            if (nodeService.checkExist(ArtifactInfo(projectId, repoName, fullPath))) {
                logger.info(
                    "package [$name] with tgz file [$fullPath] is " +
                            "already exists in repository [$projectId/$repoName], skip migration."
                )
                return 0L
            }
            try {
                measureTimeMillis {
                    response = okHttpUtil.doGet(tarball)
                    if (checkResponse(response!!)) {
                        val artifactFile = ArtifactFileFactory.build(response?.body!!.byteStream())
                        if (fullPath.endsWith(HAR_FILE_EXT)) {
                            // 保存readme,changelog文件
                            val readmeDir = NpmUtils.getReadmeDirFromTarballPath(fullPath)
                            artifactFile.getInputStream().use { storeReadmeAndChangelog(context, it, readmeDir) }
                        }
                        context.putAttribute(NPM_FILE_FULL_PATH, fullPath)
                        val nodeCreateRequest = buildMigrationNodeCreateRequest(context, artifactFile)
                        storageManager.storeArtifactFile(nodeCreateRequest, artifactFile, storageCredentials)
                        size = artifactFile.getSize()
                        artifactFile.delete()
                    }
                }.apply {
                    logger.info(
                        "migrate npm package [$name] with tgz file [$fullPath] success, elapse $this ms."
                    )
                }
            } catch (exception: IOException) {
                logger.error("http send url [$tarball] for artifact [$fullPath] failed : $exception")
                throw exception
            } finally {
                response?.body?.close()
            }
            return size
        }
    }

    private fun storeReadmeAndChangelog(context: ArtifactMigrateContext, inputStream: InputStream, readmeDir: String) {
        try {
            val (readme, changelog) = NpmUtils.getReadmeAndChangeLog(inputStream)
            readme?.let { storeReadmeOrChangeLog(context, it, "$readmeDir/$OHPM_README_FILE_NAME") }
            changelog?.let { storeReadmeOrChangeLog(context, it, "$readmeDir/$OHPM_CHANGELOG_FILE_NAME") }
        }  catch (exception: IOException) {
            logger.error(
                "Failed deploying npm readme [$readmeDir] due to : $exception"
            )
        }
    }

    private fun storeReadmeOrChangeLog(context: ArtifactMigrateContext, data: ByteArray, fullPath: String) {
        context.putAttribute(NPM_FILE_FULL_PATH, fullPath)
        val artifactFile = data.inputStream().use { ArtifactFileFactory.build(it) }
        val nodeCreateRequest = buildMigrationNodeCreateRequest(context, artifactFile)
        storageManager.storeArtifactFile(nodeCreateRequest, artifactFile, context.storageCredentials)
        artifactFile.delete()
    }

    private fun buildMigrationNodeCreateRequest(
        context: ArtifactMigrateContext,
        file: ArtifactFile
    ): NodeCreateRequest {
        val repositoryDetail = context.repositoryDetail
        val sha256 = file.getFileSha256()
        val md5 = file.getFileMd5()
        return NodeCreateRequest(
            projectId = repositoryDetail.projectId,
            repoName = repositoryDetail.name,
            folder = false,
            fullPath = context.getStringAttribute(NPM_FILE_FULL_PATH)!!,
            size = file.getSize(),
            sha256 = sha256,
            md5 = md5,
            overwrite = true,
            operator = context.userId
        )
    }

    private fun deleteVersionMetadata(context: ArtifactMigrateContext, name: String, version: String) {
        val fullPath = NpmUtils.getVersionPackageMetadataPath(name, version)
        with(context) {
            if (nodeService.checkExist(ArtifactInfo(projectId, repoName, fullPath))) {
                val nodeDeleteRequest = NodeDeleteRequest(projectId, repoName, fullPath, userId)
                nodeService.deleteNode(nodeDeleteRequest)
                logger.info(
                    "migrate package [$name] with version [$version] failed, " +
                            "delete package version metadata [$fullPath] success."
                )
            }
        }
    }

    /**
     * 检查下载响应
     */
    private fun checkResponse(response: Response): Boolean {
        if (!response.isSuccessful) {
            logger.warn("Download file from remote failed: [${response.code}]")
            return false
        }
        return true
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(NpmLocalRepository::class.java)
    }
}

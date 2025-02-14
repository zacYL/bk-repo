/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.npm.service.impl

import com.github.zafarkhaja.semver.Version
import com.tencent.bk.audit.annotations.ActionAuditRecord
import com.tencent.bk.audit.annotations.AuditAttribute
import com.tencent.bk.audit.annotations.AuditEntry
import com.tencent.bk.audit.annotations.AuditInstanceRecord
import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.api.exception.MethodNotAllowedException
import com.tencent.bkrepo.common.api.util.DecompressUtils
import com.tencent.bkrepo.common.api.util.JsonUtils.objectMapper
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.audit.ActionAuditContent
import com.tencent.bkrepo.common.artifact.audit.NODE_CREATE_ACTION
import com.tencent.bkrepo.common.artifact.audit.NODE_RESOURCE
import com.tencent.bkrepo.common.artifact.constant.ARTIFACT_INFO_KEY
import com.tencent.bkrepo.common.artifact.exception.VersionNotFoundException
import com.tencent.bkrepo.common.artifact.hash.sha1
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.pojo.RepositoryId
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.pojo.configuration.virtual.VirtualConfiguration
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactSearchContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.metadata.service.metadata.MetadataService
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo
import com.tencent.bkrepo.npm.constants.ATTRIBUTE_OCTET_STREAM_SHA1
import com.tencent.bkrepo.npm.constants.CREATED
import com.tencent.bkrepo.npm.constants.HAR_FILE_EXT
import com.tencent.bkrepo.npm.constants.HSP_TYPE
import com.tencent.bkrepo.npm.constants.HSP_TYPE_BUNDLE_APP
import com.tencent.bkrepo.npm.constants.LATEST
import com.tencent.bkrepo.npm.constants.MODIFIED
import com.tencent.bkrepo.npm.constants.NPM_FILE_FULL_PATH
import com.tencent.bkrepo.npm.constants.NPM_PACKAGE_TGZ_FILE
import com.tencent.bkrepo.npm.constants.OHPM_ARTIFACT_TYPE
import com.tencent.bkrepo.npm.constants.OHPM_CHANGELOG_FILE_NAME
import com.tencent.bkrepo.npm.constants.OHPM_DEFAULT_ARTIFACT_TYPE
import com.tencent.bkrepo.npm.constants.OHPM_DEPRECATE
import com.tencent.bkrepo.npm.constants.OHPM_PACKAGE_TYPE
import com.tencent.bkrepo.npm.constants.OHPM_PACKAGE_TYPE_HSP
import com.tencent.bkrepo.npm.constants.OHPM_README_FILE_NAME
import com.tencent.bkrepo.npm.constants.PACKAGE_JSON
import com.tencent.bkrepo.npm.constants.REQUEST_URI
import com.tencent.bkrepo.npm.constants.SEARCH_REQUEST
import com.tencent.bkrepo.npm.constants.SIZE
import com.tencent.bkrepo.npm.constants.TARBALL_FULL_PATH
import com.tencent.bkrepo.npm.exception.NpmArtifactExistException
import com.tencent.bkrepo.npm.exception.NpmArtifactNotFoundException
import com.tencent.bkrepo.npm.exception.NpmBadRequestException
import com.tencent.bkrepo.npm.exception.NpmTagNotExistException
import com.tencent.bkrepo.npm.handler.NpmDependentHandler
import com.tencent.bkrepo.npm.handler.NpmPackageHandler
import com.tencent.bkrepo.npm.model.metadata.NpmPackageMetaData
import com.tencent.bkrepo.npm.model.metadata.NpmPackageMetaData.Attachments
import com.tencent.bkrepo.npm.model.metadata.NpmVersionMetadata
import com.tencent.bkrepo.npm.model.properties.PackageProperties
import com.tencent.bkrepo.npm.pojo.NpmSearchInfoMap
import com.tencent.bkrepo.npm.pojo.NpmSearchResponse
import com.tencent.bkrepo.npm.pojo.NpmSuccessResponse
import com.tencent.bkrepo.npm.pojo.OhpmResponse
import com.tencent.bkrepo.npm.pojo.enums.NpmOperationAction
import com.tencent.bkrepo.npm.pojo.enums.NpmOperationAction.UNPUBLISH
import com.tencent.bkrepo.npm.pojo.metadata.MetadataSearchRequest
import com.tencent.bkrepo.npm.pojo.metadata.disttags.DistTags
import com.tencent.bkrepo.npm.pojo.user.OhpmDistTagRequest
import com.tencent.bkrepo.npm.service.NpmClientService
import com.tencent.bkrepo.npm.service.NpmOperationService
import com.tencent.bkrepo.npm.utils.BeanUtils
import com.tencent.bkrepo.npm.utils.NpmUtils
import com.tencent.bkrepo.npm.utils.TimeUtil
import com.tencent.bkrepo.repository.constant.CoverStrategy
import com.tencent.bkrepo.repository.pojo.metadata.MetadataModel
import com.tencent.bkrepo.repository.pojo.metadata.MetadataSaveRequest
import org.apache.commons.codec.binary.Base64
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.io.InputStream
import kotlin.system.measureTimeMillis

@Suppress("TooManyFunctions")
@Service
class NpmClientServiceImpl(
    private val npmDependentHandler: NpmDependentHandler,
    private val metadataService: MetadataService,
    private val npmPackageHandler: NpmPackageHandler,
    private val npmOperationService: NpmOperationService,
    ) : NpmClientService, AbstractNpmService() {

    @Transactional(rollbackFor = [Throwable::class])
    override fun publishOrUpdatePackage(
        userId: String,
        artifactInfo: NpmArtifactInfo,
        name: String
    ): NpmSuccessResponse {
        // 对虚拟仓库进行上传操作,需要将artifactInfo的仓库信息更改为默认部署仓库
        val realArtifactInfo = queryVirtualDeployment(artifactInfo)?.let {
            artifactInfo.copy(repoName = it) as NpmArtifactInfo
        } ?: artifactInfo
        try {
            val npmPackageMetaData =
                objectMapper.readValue(HttpContextHolder.getRequest().inputStream, NpmPackageMetaData::class.java)
            when {
                isUploadRequest(npmPackageMetaData) -> {
                    measureTimeMillis {
                        handlerPackagePublish(userId, realArtifactInfo, npmPackageMetaData)
                    }.apply {
                        logger.info(
                            "user [$userId] public npm package [$name] " +
                                "to repo [${realArtifactInfo.getRepoIdentify()}] success, elapse $this ms"
                        )
                    }
                    return NpmSuccessResponse.createEntitySuccess()
                }
                isDeprecateRequest(npmPackageMetaData) -> {
                    handlerPackageDeprecated(userId, realArtifactInfo, npmPackageMetaData)
                    return NpmSuccessResponse.updatePkgSuccess()
                }
                else -> {
                    val message = "Unknown npm put/update request, check the debug logs for further information."
                    logger.warn(message)
                    logger.debug(
                        "Unknown npm put/update request: {}",
                        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(npmPackageMetaData)
                    )
                    // 异常声明为npm模块的异常
                    throw NpmBadRequestException(message)
                }
            }
        } catch (exception: IOException) {
            logger.error("Exception while reading package metadata: ${exception.message}")
            throw exception
        }
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun ohpmStreamPublishOrUpdatePackage(
        userId: String,
        artifactInfo: NpmArtifactInfo,
        npmPackageMetaData: NpmPackageMetaData,
        artifactFile: ArtifactFile
    ): OhpmResponse {
        measureTimeMillis {
            handlerPackagePublish(userId, artifactInfo, npmPackageMetaData, artifactFile)
        }.apply {
            logger.info(
                "user [$userId] public ohpm package [${npmPackageMetaData.name}] " +
                        "to repo [${artifactInfo.getRepoIdentify()}] success, elapse $this ms"
            )
        }
        return OhpmResponse.success()
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun packageInfo(artifactInfo: NpmArtifactInfo) {
        with(artifactInfo) {
            logger.info(
                "handling query package metadata request for package [$packageName] in repo [$projectId/$repoName]"
            )
            return queryPackageInfo(artifactInfo)
        }
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun packageVersionInfo(artifactInfo: NpmArtifactInfo, name: String, version: String): NpmVersionMetadata {
        with(artifactInfo) {
            logger.info(
                "handling query package version metadata request for package [$name] " +
                    "and version [$version] in repo [$projectId/$repoName]"
            )
            if (StringUtils.equals(version, LATEST)) {
                return searchLatestVersionMetadata(artifactInfo, name)
            }
            return searchVersionMetadata(artifactInfo, name, version)
        }
    }

    @Transactional(rollbackFor = [Throwable::class])
    override fun download(artifactInfo: NpmArtifactInfo) {
        val context = ArtifactDownloadContext()
        context.putAttribute(NPM_FILE_FULL_PATH, artifactInfo.getArtifactFullPath())
        repository.download(context)
    }

    @Suppress("UNCHECKED_CAST")
    @Transactional(rollbackFor = [Throwable::class])
    override fun search(artifactInfo: NpmArtifactInfo, searchRequest: MetadataSearchRequest): NpmSearchResponse {
        val context = ArtifactSearchContext()
        context.putAttribute(SEARCH_REQUEST, searchRequest)
        val npmSearchInfoMapList = repository.search(context) as List<NpmSearchInfoMap>
        return NpmSearchResponse(npmSearchInfoMapList)
    }

    override fun getDistTags(artifactInfo: NpmArtifactInfo, name: String): DistTags {
        with(artifactInfo) {
            logger.info("handling get distTags request for package [$name] in repo [$projectId/$repoName]")
            val packageMetaData = queryPackageInfo(artifactInfo, name, false)
            return packageMetaData.distTags.getMap()
        }
    }

    override fun addDistTags(userId: String, artifactInfo: NpmArtifactInfo, name: String, tag: String) {
        // 对虚拟仓库添加dist tag时,转换为添加到默认部署仓库
        val realArtifactInfo = queryVirtualDeployment(artifactInfo)?.let {
            artifactInfo.copy(repoName = it) as NpmArtifactInfo
        } ?: artifactInfo
        logger.info(
            "handling add distTags [$tag] request for package [$name] " +
                "in repo [${realArtifactInfo.getRepoIdentify()}]"
        )
        val packageMetaData = queryPackageInfo(realArtifactInfo, name, false)
        val repoId = RepositoryId(artifactInfo.projectId, artifactInfo.repoName)
        val version = if (ArtifactContextHolder.getRepoDetail(repoId)?.type == RepositoryType.OHPM) {
            objectMapper.readValue(HttpContextHolder.getRequest().inputStream, OhpmDistTagRequest::class.java).version
        } else {
            objectMapper.readValue(HttpContextHolder.getRequest().inputStream, String::class.java)
        }
        if (packageMetaData.versions.map.containsKey(version)) {
            packageMetaData.distTags.set(tag, version)
            doPackageFileUpload(userId, realArtifactInfo, packageMetaData)
        }
    }

    override fun deleteDistTags(userId: String, artifactInfo: NpmArtifactInfo, name: String, tag: String) {
        // 对虚拟仓库删除dist tag时,转换为在默认部署仓库删除
        val realArtifactInfo = queryVirtualDeployment(artifactInfo)?.let {
            artifactInfo.copy(repoName = it) as NpmArtifactInfo
        } ?: artifactInfo
        logger.info(
            "handling delete distTags [$tag] request for package [$name] " +
                "in repo [${realArtifactInfo.getRepoIdentify()}]"
        )
        if (LATEST == tag) {
            logger.warn(
                "dist tag for [latest] with package [$name] " +
                    "in repo [${realArtifactInfo.getRepoIdentify()}] cannot be deleted."
            )
            return
        }
        val packageMetaData = queryPackageInfo(realArtifactInfo, name, false)
        packageMetaData.distTags.getMap().remove(tag)
        doPackageFileUpload(userId, realArtifactInfo, packageMetaData)
    }

    override fun updatePackage(userId: String, artifactInfo: NpmArtifactInfo, name: String) {
        val realArtifactInfo = queryVirtualDeployment(artifactInfo)?.let {
            artifactInfo.copy(repoName = it) as NpmArtifactInfo
        } ?: artifactInfo
        logger.info(
            "handling update package request for package [$name] in repo [${realArtifactInfo.getRepoIdentify()}]"
        )
        val packageMetadata =
            objectMapper.readValue(HttpContextHolder.getRequest().inputStream, NpmPackageMetaData::class.java)
        doPackageFileUpload(userId, realArtifactInfo, packageMetadata)
    }

    override fun deleteVersion(artifactInfo: NpmArtifactInfo) {
        with(artifactInfo) {
            logger.info("npm delete package version request: [$this]")
            val ohpm = ArtifactContextHolder.getRepoDetail()?.type == RepositoryType.OHPM
            val packageKey = NpmUtils.packageKey(packageName, ohpm)
            val versionInfo = packageService.findVersionByName(projectId, repoName, packageKey, version!!)
                ?: throw VersionNotFoundException("$packageKey/$version")
            val context = ArtifactRemoveContext()
            context.putAttribute(TARBALL_FULL_PATH, versionInfo.contentPath!!)
            // 删除包管理中对应的version
            npmPackageHandler.deleteVersion(context.userId, packageName, version!!, artifactInfo)
            repository.remove(context)
            logger.info("userId [${context.userId}] delete version [$version] for package [$packageName] success.")
        }
    }

    override fun deletePackage(artifactInfo: NpmArtifactInfo) {
        with(artifactInfo) {
            logger.info("npm delete package request: [$this]")
            val context = ArtifactRemoveContext(artifact = artifactInfo)
            val pkgMetadata = npmOperationService.loadPackageMetadata(context)
            if (pkgMetadata != null) {
                checkOhpmDependentsAndDeprecate(context.userId, artifactInfo, pkgMetadata, null)
                val ohpm = context.repositoryDetail.type == RepositoryType.OHPM
                npmDependentHandler.updatePackageDependents(
                    context.userId, this, pkgMetadata, UNPUBLISH, ohpm
                )
            } else {
                logger.warn("fail to load package metadata while delete package[$this]")
            }
            npmPackageHandler.deletePackage(context.userId, packageName, this)
            repository.remove(context)
            logger.info("userId [${context.userId}] delete package [$packageName] success.")
        }
    }

    override fun checkOhpmDependentsAndDeprecate(
        userId: String,
        artifactInfo: NpmArtifactInfo,
        packageMetaData: NpmPackageMetaData,
        version: String?
    ) {
        val projectId = artifactInfo.projectId
        val repoName = artifactInfo.repoName
        val name = packageMetaData.name!!
        val ohpm = ArtifactContextHolder.getRepoDetail()!!.type == RepositoryType.OHPM
        if (!ohpm || !npmDependentHandler.existsPackageDependents(projectId, repoName, name, ohpm)) {
            return
        }

        packageMetaData.versions.map.forEach {
            if (version == null || version == it.key) {
                it.value.set(OHPM_DEPRECATE, true)
            }
        }
        doPackageFileUpload(userId, artifactInfo, packageMetaData)
        throw NpmBadRequestException("The OHPM package \"${name}\" has been depended on by other components.")
    }

    private fun searchLatestVersionMetadata(artifactInfo: NpmArtifactInfo, name: String): NpmVersionMetadata {
        logger.info("handling query latest version metadata request for package [$name]")
        try {
            val context = ArtifactQueryContext()
            val packageFullPath = NpmUtils.getPackageMetadataPath(name)
            context.putAttribute(NPM_FILE_FULL_PATH, packageFullPath)
            context.putAttribute(REQUEST_URI, name)
            val inputStream =
                repository.query(context) as? InputStream
                    ?: throw NpmArtifactNotFoundException("document not found")
            val npmPackageMetaData = inputStream.use { objectMapper.readValue(it, NpmPackageMetaData::class.java) }
            val distTags = npmPackageMetaData.distTags
            if (!distTags.getMap().containsKey(LATEST)) {
                val message =
                    "the dist tag [latest] is not found in package [$name] in repo [${artifactInfo.getRepoIdentify()}]"
                logger.error(message)
                throw NpmTagNotExistException(message)
            }
            val latestVersion = distTags.getMap()[LATEST]!!
            return searchVersionMetadata(artifactInfo, name, latestVersion)
        } catch (exception: IOException) {
            val message = "Unable to get npm metadata for package $name and version latest"
            logger.error(message)
            throw NpmBadRequestException(message)
        }
    }

    private fun searchVersionMetadata(
        artifactInfo: NpmArtifactInfo,
        name: String,
        version: String
    ): NpmVersionMetadata {
        try {
            val context = ArtifactQueryContext()
            val packageFullPath = NpmUtils.getVersionPackageMetadataPath(name, version)
            context.putAttribute(NPM_FILE_FULL_PATH, packageFullPath)
            context.putAttribute(REQUEST_URI, "$name/$version")
            val inputStream =
                repository.query(context) as? InputStream
                    ?: throw NpmArtifactNotFoundException("document not found")
            val versionMetadata = inputStream.use { objectMapper.readValue(it, NpmVersionMetadata::class.java) }
            modifyVersionMetadataTarball(artifactInfo, name, versionMetadata)
            return versionMetadata
        } catch (exception: IOException) {
            val message = "Unable to get npm metadata for package $name and version $version"
            logger.error(message)
            throw NpmBadRequestException(message)
        }
    }

    @AuditEntry(
        actionId = NODE_CREATE_ACTION
    )
    @ActionAuditRecord(
        actionId = NODE_CREATE_ACTION,
        instance = AuditInstanceRecord(
            resourceType = NODE_RESOURCE,
            instanceIds = "#artifactInfo?.getArtifactFullPath()",
            instanceNames = "#artifactInfo?.getArtifactFullPath()"
        ),
        attributes = [
            AuditAttribute(name = ActionAuditContent.PROJECT_CODE_TEMPLATE, value = "#artifactInfo?.projectId"),
            AuditAttribute(name = ActionAuditContent.REPO_NAME_TEMPLATE, value = "#artifactInfo?.repoName")
        ],
        scopeId = "#artifactInfo?.projectId",
        content = ActionAuditContent.NODE_UPLOAD_CONTENT
    )
    private fun handlerPackagePublish(
        userId: String,
        artifactInfo: NpmArtifactInfo,
        npmPackageMetaData: NpmPackageMetaData,
        artifactFile: ArtifactFile? = null
    ) {
        if (npmPackageMetaData.attachments == null && artifactFile == null) {
            val message = "Missing attachments with tarball data, aborting upload for '${npmPackageMetaData.name}'"
            logger.warn(message)
            throw NpmBadRequestException(message)
        }
        try {
            var size = 0L
            if (artifactFile != null) {
                size = artifactFile.getSize()
            } else {
                npmPackageMetaData.attachments!!.getMap().values.forEach { size += it.length!!.toLong() }
            }
            val ohpm = ArtifactContextHolder.getRepoDetail()!!.type == RepositoryType.OHPM
            if (ohpm) {
                resolveOhpm(npmPackageMetaData)
            }
            handlerAttachmentsUpload(userId, artifactInfo, npmPackageMetaData, artifactFile)
            handlerPackageFileUpload(userId, artifactInfo, npmPackageMetaData, size, ohpm)
            handlerVersionFileUpload(userId, artifactInfo, npmPackageMetaData, size)
            if (npmPackageMetaData.distTags.getMap().containsKey(LATEST)) {
                npmDependentHandler.updatePackageDependents(
                    userId,
                    artifactInfo,
                    npmPackageMetaData,
                    NpmOperationAction.PUBLISH,
                    ohpm
                )
            }
            val versionMetadata = npmPackageMetaData.versions.map.values.iterator().next()
            npmPackageHandler.createVersion(userId, artifactInfo, versionMetadata, size, ohpm)
        } catch (exception: IOException) {
            val version = NpmUtils.getLatestVersionFormDistTags(npmPackageMetaData.distTags)
            logger.error(
                "userId [$userId] publish package [${npmPackageMetaData.name}] for version [$version] " +
                    "to repo [${artifactInfo.projectId}/${artifactInfo.repoName}] failed."
            )
        }
    }

    private fun resolveOhpm(npmPackageMetaData: NpmPackageMetaData) {
        npmPackageMetaData.versions.map.forEach {
            val version = it.value
            resolveOhpmHsp(version)
            val hspType = npmPackageMetaData.any()[HSP_TYPE]?.toString()
            if (!hspType.isNullOrEmpty()) {
                version.set(HSP_TYPE, hspType)
            }
            if (version.any()[OHPM_ARTIFACT_TYPE] == null) {
                // OpenHarmony包制品类型，有两个选项：original、obfuscation
                // original：源码，即发布源码(.ts/.ets)；obfuscation：混淆代码，即源码经过混淆之后发布上传
                // 默认为original
                version.set(OHPM_ARTIFACT_TYPE, OHPM_DEFAULT_ARTIFACT_TYPE)
            }
        }
    }

    private fun handlerPackageFileUpload(
        userId: String,
        artifactInfo: NpmArtifactInfo,
        npmPackageMetaData: NpmPackageMetaData,
        size: Long,
        ohpm: Boolean,
    ) {
        with(artifactInfo) {
            val packageKey = NpmUtils.packageKeyByRepoType(npmPackageMetaData.name.orEmpty())
            val gmtTime = TimeUtil.getGMTTime()
            val npmMetadata = npmPackageMetaData.versions.map.values.iterator().next()
            if (!npmMetadata.dist!!.any().containsKey(SIZE)) {
                npmMetadata.dist!!.set(SIZE, size)
            }
            // 第一次上传
            if (!packageExist(projectId, repoName, packageKey)) {
                if (ohpm) {
                    npmPackageMetaData.rev = npmPackageMetaData.versions.map.size.toString()
                }
                npmPackageMetaData.time.add(CREATED, gmtTime)
                npmPackageMetaData.time.add(MODIFIED, gmtTime)
                npmPackageMetaData.time.add(npmMetadata.version!!, gmtTime)
                doPackageFileUpload(userId, artifactInfo, npmPackageMetaData)
                return
            }
            val originalPackageInfo = queryPackageInfo(artifactInfo, npmPackageMetaData.name!!, false)
            originalPackageInfo.versions.map.putAll(npmPackageMetaData.versions.map)
            originalPackageInfo.distTags.getMap().putAll(npmPackageMetaData.distTags.getMap())
            originalPackageInfo.time.add(MODIFIED, gmtTime)
            originalPackageInfo.time.add(npmMetadata.version!!, gmtTime)
            if (ohpm) {
                NpmUtils.updateLatestVersion(originalPackageInfo)
                originalPackageInfo.rev = originalPackageInfo.versions.map.size.toString()
            }
            doPackageFileUpload(userId, artifactInfo, originalPackageInfo)
        }
    }

    private fun doPackageFileUpload(
        userId: String,
        artifactInfo: NpmArtifactInfo,
        npmPackageMetaData: NpmPackageMetaData
    ) {
        with(artifactInfo) {
            val fullPath = NpmUtils.getPackageMetadataPath(npmPackageMetaData.name!!)
            val inputStream = objectMapper.writeValueAsString(npmPackageMetaData).byteInputStream()
            val artifactFile = inputStream.use { ArtifactFileFactory.build(it) }
            val context = ArtifactUploadContext(artifactFile)
            context.putAttribute(NPM_FILE_FULL_PATH, fullPath)
            repository.upload(context).also {
                logger.info(
                    "user [$userId] upload npm package metadata file [$fullPath] " +
                        "into repo [$projectId/$repoName] success."
                )
            }
            artifactFile.delete()
        }
    }

    private fun handlerVersionFileUpload(
        userId: String,
        artifactInfo: NpmArtifactInfo,
        npmPackageMetaData: NpmPackageMetaData,
        size: Long
    ) {
        with(artifactInfo) {
            val npmMetadata = npmPackageMetaData.versions.map.values.iterator().next()
            if (!npmMetadata.dist!!.any().containsKey(SIZE)) {
                npmMetadata.dist!!.set(SIZE, size)
            }
            val fullPath = NpmUtils.getVersionPackageMetadataPath(npmMetadata.name!!, npmMetadata.version!!)
            val inputStream = objectMapper.writeValueAsString(npmMetadata).byteInputStream()
            val artifactFile = inputStream.use { ArtifactFileFactory.build(it) }
            val context = ArtifactUploadContext(artifactFile)
            context.putAttribute(NPM_FILE_FULL_PATH, fullPath)
            // ohpm包没有shasum字段
            npmMetadata.dist?.shasum?.let { context.putAttribute(ATTRIBUTE_OCTET_STREAM_SHA1, it) }
            repository.upload(context).also {
                logger.info(
                    "user [$userId] upload npm package version metadata file [$fullPath] " +
                        "into repo [$projectId/$repoName] success."
                )
            }
            artifactFile.delete()
        }
    }

    private fun handlerAttachmentsUpload(
        userId: String,
        artifactInfo: NpmArtifactInfo,
        npmPackageMetaData: NpmPackageMetaData,
        artifactFile: ArtifactFile? = null,
    ) {
        val versionMetadata = npmPackageMetaData.versions.map.values.iterator().next()
        val packageKey = NpmUtils.packageKeyByRepoType(versionMetadata.name.orEmpty())
        val version = versionMetadata.version.orEmpty()
        with(artifactInfo) {
            // 判断包版本是否存在 如果该版本先前发布过，也不让再次发布该版本
            val coverStrategy = ArtifactContextHolder.getRepoDetailOrNull()?.coverStrategy ?: CoverStrategy.DISABLE
            logger.info("projectId[$projectId] npm repo[$repoName] coverStrategy[$coverStrategy]")
            if ((packageVersionExist(projectId, repoName, packageKey, version) ||
                packageHistoryVersionExist(projectId, repoName, packageKey, version)) &&
                coverStrategy == CoverStrategy.UNCOVER) {
                throw NpmArtifactExistException(
                    "You cannot publish over the previously published versions: ${versionMetadata.version}." +
                        "projectId[$projectId] repo[$repoName] cover strategy uncover."
                )
            }
        }

        if (artifactFile == null) {
            // 从package metadata中获取tarball数据
            npmPackageMetaData.attachments!!.getMap().forEach { attachment ->
                val fullPath = "${versionMetadata.name}/-/${attachment.key}"
                val inputStream = tgzContentToInputStream(attachment.value.data!!)
                val attachmentArtifactFile = inputStream.use { ArtifactFileFactory.build(it) }
                handlerAttachmentsUpload(
                    userId,
                    artifactInfo,
                    attachment.value.contentType!!,
                    attachment.value.length!!,
                    attachmentArtifactFile,
                    fullPath
                )
                attachmentArtifactFile.delete()
            }
            // 将attachments移除
            npmPackageMetaData.attachments = null
            return
        }

        // 从artifactFile中获取tarball数据
        val ohpmPackageType = npmPackageMetaData.any()[OHPM_PACKAGE_TYPE]
        val hspType = npmPackageMetaData.any()[HSP_TYPE]
        if (ohpmPackageType != OHPM_PACKAGE_TYPE_HSP || hspType != HSP_TYPE_BUNDLE_APP) {
            // har
            val fullPath = NpmUtils.getTgzPath(versionMetadata.name!!, version, true, HAR_FILE_EXT)
            handlerAttachmentsUpload(
                userId,
                artifactInfo,
                MediaTypes.APPLICATION_OCTET_STREAM,
                artifactFile.getSize().toInt(),
                artifactFile,
                fullPath
            )
            return
        }

        // hsp
        artifactFile.getInputStream().use { artifactInputStream ->
            val tgz = object : TarArchiveInputStream(GzipCompressorInputStream(artifactInputStream)) {
                // 外层artifactInputStream会关闭流，此处不关闭，避免StreamArtifactFile中关闭流导致后续entry读取失败
                override fun close() {}
            }
            do {
                val entry = tgz.nextEntry ?: break
                val fileNameExt = entry.name.substring(entry.name.length - 4, entry.name.length)
                val fullPath = NpmUtils.getTgzPath(versionMetadata.name!!, version, true, fileNameExt)
                val tarballArtifactFile = ArtifactFileFactory.build(tgz, entry.size)
                handlerAttachmentsUpload(
                    userId,
                    artifactInfo,
                    MediaTypes.APPLICATION_OCTET_STREAM,
                    entry.size.toInt(),
                    tarballArtifactFile,
                    fullPath
                )
                tarballArtifactFile.delete()
            } while (true)
        }
    }

    private fun handlerAttachmentsUpload(
        userId: String,
        artifactInfo: NpmArtifactInfo,
        contentType: String,
        contentLength: Int,
        artifactFile: ArtifactFile,
        fullPath: String,
    ) {
        val projectId = artifactInfo.projectId
        val repoName = artifactInfo.repoName
        logger.info("user [$userId] deploying npm package [$fullPath] into repo [$projectId/$repoName]")
        try {
            if (fullPath.endsWith(HAR_FILE_EXT)) {
                // 保存readme,changelog文件
                val readmeDir = NpmUtils.getReadmeDirFromTarballPath(fullPath)
                artifactFile.getInputStream().use { handlerOhpmReadmeAndChangelogUpload(it, readmeDir) }
            }
            val context = ArtifactUploadContext(artifactFile)
            context.putAttribute(NPM_FILE_FULL_PATH, fullPath)
            context.putAttribute("attachments.content_type", contentType)
            context.putAttribute("attachments.length", contentLength)
            context.putAttribute("name", NPM_PACKAGE_TGZ_FILE)
            // context.putAttribute(NPM_METADATA, buildProperties(versionMetadata))
            repository.upload(context)
        } catch (exception: IOException) {
            logger.error(
                "Failed deploying npm package [$fullPath] into repo [$projectId/$repoName] due to : $exception"
            )
        }
    }

    private fun handlerOhpmReadmeAndChangelogUpload(inputStream: InputStream, readmeDir: String) {
        try {
            val (readme, changelog) = NpmUtils.getReadmeAndChangeLog(inputStream)
            readme?.let { uploadReadmeOrChangeLog(it, "$readmeDir/$OHPM_README_FILE_NAME") }
            changelog?.let { uploadReadmeOrChangeLog(it, "$readmeDir/$OHPM_CHANGELOG_FILE_NAME") }
        }  catch (exception: IOException) {
            logger.error(
                "Failed deploying npm readme [$readmeDir] due to : $exception"
            )
        }
    }

    private fun uploadReadmeOrChangeLog(byteArray: ByteArray, fullPath: String) {
        val artifactFile = byteArray.inputStream().use { ArtifactFileFactory.build(it) }
        val context = ArtifactUploadContext(artifactFile)
        context.putAttribute(NPM_FILE_FULL_PATH, fullPath)
        context.putAttribute("name", NPM_PACKAGE_TGZ_FILE)
        ArtifactContextHolder.getRepository().upload(context)
        artifactFile.delete()
    }

    private fun buildProperties(npmVersionMetadata: NpmVersionMetadata?): List<MetadataModel> {
        return npmVersionMetadata?.let {
            val npmProperties = PackageProperties(
                it.license,
                it.keywords,
                it.name!!,
                it.version!!,
                it.maintainers,
                it.any()["deprecated"] as? String
            )
            BeanUtils.beanToMap(npmProperties).filterValues { value -> value != null }.map { metadata ->
                MetadataModel(key = metadata.key, value = metadata.value!!)
            }
        } ?: emptyList()
    }

    private fun tgzContentToInputStream(data: String): InputStream {
        return Base64.decodeBase64(data).inputStream()
    }

    private fun handlerPackageDeprecated(
        userId: String,
        artifactInfo: NpmArtifactInfo,
        npmPackageMetaData: NpmPackageMetaData
    ) {
        logger.info(
            "userId [$userId] handler deprecated request: [$npmPackageMetaData] " +
                "in repo [${artifactInfo.projectId}]"
        )
        doPackageFileUpload(userId, artifactInfo, npmPackageMetaData)
        // 元数据增加过期信息
        val iterator = npmPackageMetaData.versions.map.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val tgzFullPath = NpmUtils.getTarballPathByRepoType(npmPackageMetaData.name!!, entry.key)
            if (entry.value.any().containsKey("deprecated")) {
                metadataService.saveMetadata(
                    MetadataSaveRequest(
                        projectId = artifactInfo.projectId,
                        repoName = artifactInfo.repoName,
                        fullPath = tgzFullPath,
                        nodeMetadata = buildProperties(entry.value),
                        operator = userId
                    )
                )
            }
        }
    }

    /**
     *  从[artifactInfo]查询是否为虚拟仓库且是否设置了默认部署仓库
     */
    private fun queryVirtualDeployment(artifactInfo: NpmArtifactInfo): String? {
        val repoId = RepositoryId(artifactInfo.projectId, artifactInfo.repoName)
        val repoDetail = ArtifactContextHolder.getRepoDetail(repoId)
        return if (repoDetail.category == RepositoryCategory.VIRTUAL) {
            (repoDetail.configuration as VirtualConfiguration).deploymentRepo.takeUnless { it.isNullOrBlank() }
                ?: throw MethodNotAllowedException()
        } else null
    }

    override fun upload(projectId: String, repoName: String, file: MultipartFile) {
        val repoType = ArtifactContextHolder.getRepoDetail(RepositoryId(projectId, repoName)).type
        require(repoType == RepositoryType.NPM)
        val bytes = file.bytes
        val packageVersion = DecompressUtils.tryArchiverWithCompressor<NpmVersionMetadata, ByteArray>(
            bytes.inputStream(),
            callbackPre = { it.name.endsWith(PACKAGE_JSON) },
            callback = { stream, _ -> stream.readBytes() },
            handleResult = { _, packageJson, _ -> packageJson?.readPackageJson(bytes) },
            callbackPost = { _, _ -> false }
        ) ?: throw NpmBadRequestException("Invalid npm package")
        with(packageVersion) {
            val artifactInfo = NpmArtifactInfo(projectId, repoName, name!!, version)
            HttpContextHolder.getRequest().setAttribute(ARTIFACT_INFO_KEY, artifactInfo)
            handlerPackagePublish(SecurityUtils.getUserId(), artifactInfo, toNpmPackageMetaData(artifactInfo, bytes))
        }
    }

    private fun NpmVersionMetadata.toNpmPackageMetaData(artifactInfo: NpmArtifactInfo, tgz: ByteArray) = with(this) {
        return@with NpmPackageMetaData().apply {
            this.name = this@with.name
            this.description = this@with.description
            this.versions.map = mutableMapOf(this@with.version!! to this@with)
            val attachment = NpmPackageMetaData.Attachment().apply {
                this.contentType = ""//不为空就行
                val encode = Base64.encodeBase64(tgz)
                this.data = org.apache.commons.codec.binary.StringUtils.newStringUsAscii(encode)
                this.length = encode.size
            }
            this.attachments = Attachments().apply { add("$name-${this@with.version}.tgz", attachment) }
            // 获取最新版本包
            val repoId = RepositoryId(artifactInfo.projectId, artifactInfo.repoName)
            val repoType = ArtifactContextHolder.getRepoDetail(repoId).type
            val packageKey = NpmUtils.packageKeyByRepoType(artifactInfo.packageName, repoType)
            val latest = packageService
                .findLatestBySemVer(
                    artifactInfo.projectId,
                    artifactInfo.repoName,
                    packageKey
                )?.name

            // 检查获取的最新版本是否为空或当前版本比最新版本更高
            // 如果是，则将当前版本设置为最新版本
            if (latest.isNullOrBlank() || Version.valueOf(this@with.version!!) > Version.valueOf(latest)) {
                this.distTags.set(LATEST, this@with.version!!)
            }
        }
    }

    private fun ByteArray.readPackageJson(tgz: ByteArray): NpmVersionMetadata? {
        return inputStream().readJsonString<NpmVersionMetadata?>()?.apply {
            val name = name ?: return null
            val version = version ?: return null
            this.dist = NpmVersionMetadata.Dist().apply {
                set(SIZE, tgz.size)
                this.shasum = tgz.inputStream().sha1()
                this.tarball = NpmUtils.getTarballFullPath(
                    name,
                    version,
                    repeatedScope = NpmUtils.isScopeName(name),
                )
            }
        }
    }

    companion object {

        fun isUploadRequest(npmPackageMetaData: NpmPackageMetaData): Boolean {
            val attachments = npmPackageMetaData.attachments
            return attachments != null && attachments.getMap().entries.isNotEmpty()
        }

        fun isDeprecateRequest(npmPackageMetaData: NpmPackageMetaData): Boolean {
            val versions = npmPackageMetaData.versions
            val iterator = versions.map.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val npmMetadata = entry.value
                if (npmMetadata.any().containsKey("deprecated")) {
                    return true
                }
            }

            return false
        }

        private val logger: Logger = LoggerFactory.getLogger(NpmClientServiceImpl::class.java)
    }
}

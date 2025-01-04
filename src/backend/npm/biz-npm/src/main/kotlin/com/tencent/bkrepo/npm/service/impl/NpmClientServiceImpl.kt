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

import com.tencent.bkrepo.common.api.exception.MethodNotAllowedException
import com.tencent.bkrepo.common.api.util.DecompressUtils
import com.tencent.bkrepo.common.api.util.JsonUtils.objectMapper
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.artifact.constant.ARTIFACT_INFO_KEY
import com.tencent.bkrepo.common.artifact.exception.VersionNotFoundException
import com.tencent.bkrepo.common.artifact.hash.sha1
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.pojo.configuration.virtual.VirtualConfiguration
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactSearchContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.artifact.util.version.SemVersionParser.parse
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo
import com.tencent.bkrepo.npm.constants.ATTRIBUTE_OCTET_STREAM_SHA1
import com.tencent.bkrepo.npm.constants.CREATED
import com.tencent.bkrepo.npm.constants.LATEST
import com.tencent.bkrepo.npm.constants.MODIFIED
import com.tencent.bkrepo.npm.constants.NPM_FILE_FULL_PATH
import com.tencent.bkrepo.npm.constants.NPM_PACKAGE_TGZ_FILE
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
import com.tencent.bkrepo.npm.pojo.enums.NpmOperationAction
import com.tencent.bkrepo.npm.pojo.metadata.MetadataSearchRequest
import com.tencent.bkrepo.npm.pojo.metadata.disttags.DistTags
import com.tencent.bkrepo.npm.service.NpmClientService
import com.tencent.bkrepo.npm.service.NpmOperationService
import com.tencent.bkrepo.npm.utils.BeanUtils
import com.tencent.bkrepo.npm.utils.NpmUtils
import com.tencent.bkrepo.npm.utils.TimeUtil
import com.tencent.bkrepo.repository.api.MetadataClient
import com.tencent.bkrepo.repository.constant.CoverStrategy
import com.tencent.bkrepo.repository.pojo.metadata.MetadataModel
import com.tencent.bkrepo.repository.pojo.metadata.MetadataSaveRequest
import org.apache.commons.codec.binary.Base64
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
    private val metadataClient: MetadataClient,
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
        val version = objectMapper.readValue(HttpContextHolder.getRequest().inputStream, String::class.java)
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
            val packageKey = PackageKeys.ofNpm(packageName)
            val versionInfo = packageClient.findVersionByName(projectId, repoName, packageKey, version!!).data
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
            npmPackageHandler.deletePackage(context.userId, packageName, this)
            repository.remove(context)
            val pkgMetadata = npmOperationService.loadPackageMetadata(context)
            if (pkgMetadata != null) {
                npmDependentHandler.updatePackageDependents(
                    context.userId, this, pkgMetadata, NpmOperationAction.UNPUBLISH
                )
            } else {
                logger.warn("fail to load package metadata while delete package[$this]")
            }
            logger.info("userId [${context.userId}] delete package [$packageName] success.")
        }
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

    private fun handlerPackagePublish(
        userId: String,
        artifactInfo: NpmArtifactInfo,
        npmPackageMetaData: NpmPackageMetaData
    ) {
        val attachments = npmPackageMetaData.attachments
        attachments ?: run {
            val message = "Missing attachments with tarball data, aborting upload for '${npmPackageMetaData.name}'"
            logger.warn(message)
            throw NpmBadRequestException(message)
        }
        try {
            val size = attachments.getMap().values.iterator().next().length!!.toLong()
            handlerAttachmentsUpload(userId, artifactInfo, npmPackageMetaData)
            handlerPackageFileUpload(userId, artifactInfo, npmPackageMetaData, size)
            handlerVersionFileUpload(userId, artifactInfo, npmPackageMetaData, size)
            if (npmPackageMetaData.distTags.getMap().containsKey(LATEST)) {
                npmDependentHandler.updatePackageDependents(
                    userId,
                    artifactInfo,
                    npmPackageMetaData,
                    NpmOperationAction.PUBLISH
                )
            }
            val versionMetadata = npmPackageMetaData.versions.map.values.iterator().next()
            npmPackageHandler.createVersion(userId, artifactInfo, versionMetadata, size)
        } catch (exception: IOException) {
            val version = NpmUtils.getLatestVersionFormDistTags(npmPackageMetaData.distTags)
            logger.error(
                "userId [$userId] publish package [${npmPackageMetaData.name}] for version [$version] " +
                    "to repo [${artifactInfo.projectId}/${artifactInfo.repoName}] failed."
            )
        }
    }

    private fun handlerPackageFileUpload(
        userId: String,
        artifactInfo: NpmArtifactInfo,
        npmPackageMetaData: NpmPackageMetaData,
        size: Long
    ) {
        with(artifactInfo) {
            val packageKey = PackageKeys.ofNpm(npmPackageMetaData.name.orEmpty())
            val gmtTime = TimeUtil.getGMTTime()
            val npmMetadata = npmPackageMetaData.versions.map.values.iterator().next()
            if (!npmMetadata.dist!!.any().containsKey(SIZE)) {
                npmMetadata.dist!!.set(SIZE, size)
            }
            // 第一次上传
            if (!packageExist(projectId, repoName, packageKey)) {
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
            context.putAttribute(ATTRIBUTE_OCTET_STREAM_SHA1, npmMetadata.dist?.shasum!!)
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
        npmPackageMetaData: NpmPackageMetaData
    ) {
        val attachmentEntry = npmPackageMetaData.attachments!!.getMap().entries.iterator().next()
        val versionMetadata = npmPackageMetaData.versions.map.values.iterator().next()
        val fullPath = "${versionMetadata.name}/-/${attachmentEntry.key}"
        val packageKey = PackageKeys.ofNpm(versionMetadata.name.orEmpty())
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
            logger.info("user [$userId] deploying npm package [$fullPath] into repo [$projectId/$repoName]")
            try {
                val inputStream = tgzContentToInputStream(attachmentEntry.value.data!!)
                val artifactFile = inputStream.use { ArtifactFileFactory.build(it) }
                val context = ArtifactUploadContext(artifactFile)
                context.putAttribute(NPM_FILE_FULL_PATH, fullPath)
                context.putAttribute("attachments.content_type", attachmentEntry.value.contentType!!)
                context.putAttribute("attachments.length", attachmentEntry.value.length!!)
                context.putAttribute("name", NPM_PACKAGE_TGZ_FILE)
                // context.putAttribute(NPM_METADATA, buildProperties(versionMetadata))
                // 将attachments移除
                npmPackageMetaData.attachments = null
                repository.upload(context)
                artifactFile.delete()
            } catch (exception: IOException) {
                logger.error(
                    "Failed deploying npm package [$fullPath] into repo [$projectId/$repoName] due to : $exception"
                )
            }
        }
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
            BeanUtils.beanToMap(npmProperties).mapNotNull  { metadata ->
                metadata.value?.let { MetadataModel(key = metadata.key, value = it) }
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
            val tgzFullPath = NpmUtils.getTarballFullPath(npmPackageMetaData.name!!, entry.key)
            if (entry.value.any().containsKey("deprecated")) {
                metadataClient.saveMetadata(
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
        val repoDetail = repositoryClient.getRepoDetail(artifactInfo.projectId, artifactInfo.repoName).data!!
        return if (repoDetail.category == RepositoryCategory.VIRTUAL) {
            (repoDetail.configuration as VirtualConfiguration).deploymentRepo.takeUnless { it.isNullOrBlank() }
                ?: throw MethodNotAllowedException()
        } else null
    }

    override fun upload(projectId: String, repoName: String, file: MultipartFile) {
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

    private fun ByteArray.readPackageJson(tgz: ByteArray): NpmVersionMetadata? {
        return inputStream().readJsonString<NpmVersionMetadata?>()?.apply {
            this.dist = NpmVersionMetadata.Dist().apply {
                set(SIZE, tgz.size)
                this.shasum = tgz.inputStream().sha1()
            }
        }
    }

    private fun NpmVersionMetadata.toNpmPackageMetaData(artifactInfo: NpmArtifactInfo, tgz: ByteArray) = with(this) {
        return@with NpmPackageMetaData().apply {
            this.name = this@with.name
            this.description = this@with.description
            this.versions.map = mutableMapOf(version!! to this@with)
            val attachment = NpmPackageMetaData.Attachment().apply {
                this.contentType = ""//不为空就行
                val encode = Base64.encodeBase64(tgz)
                this.data = org.apache.commons.codec.binary.StringUtils.newStringUsAscii(encode)
                this.length = encode.size
            }
            this.attachments = Attachments().apply { add("$name-$version.tgz", attachment) }
            // 获取最新版本包
            val latest = packageClient
                .findLatestBySemVer(
                    artifactInfo.projectId,
                    artifactInfo.repoName,
                    PackageKeys.ofNpm(artifactInfo.packageName)
                )
                .data?.name

            // 检查获取的最新版本是否为空或当前版本比最新版本更高
            // 如果是，则将当前版本设置为最新版本
            if (latest.isNullOrBlank() || parse(version!!) > parse(latest)) {
                this.distTags.set(LATEST, version!!)
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

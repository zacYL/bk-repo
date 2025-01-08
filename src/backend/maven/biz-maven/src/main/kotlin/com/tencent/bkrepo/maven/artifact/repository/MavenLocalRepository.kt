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

package com.tencent.bkrepo.maven.artifact.repository

import com.tencent.bkrepo.common.api.constant.StringPool.SLASH
import com.tencent.bkrepo.common.api.constant.ensureSuffix
import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.exception.NodeNotFoundException
import com.tencent.bkrepo.common.artifact.exception.VersionNotFoundException
import com.tencent.bkrepo.common.artifact.hash.sha1
import com.tencent.bkrepo.common.artifact.hash.sha512
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.pojo.RepositoryIdentify
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.pojo.configuration.virtual.VirtualConfiguration
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.local.LocalRepository
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactChannel
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.service.util.HeaderUtils
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.maven.artifact.MavenArtifactInfo
import com.tencent.bkrepo.maven.artifact.MavenDeleteArtifactInfo
import com.tencent.bkrepo.maven.constants.FULL_PATH
import com.tencent.bkrepo.maven.constants.MAVEN_METADATA_FILE_NAME
import com.tencent.bkrepo.maven.constants.METADATA_KEY_ARTIFACT_ID
import com.tencent.bkrepo.maven.constants.METADATA_KEY_CLASSIFIER
import com.tencent.bkrepo.maven.constants.METADATA_KEY_GROUP_ID
import com.tencent.bkrepo.maven.constants.METADATA_KEY_MODEL_VERSION
import com.tencent.bkrepo.maven.constants.METADATA_KEY_NAME
import com.tencent.bkrepo.maven.constants.METADATA_KEY_PACKAGING
import com.tencent.bkrepo.maven.constants.METADATA_KEY_VERSION
import com.tencent.bkrepo.maven.constants.PACKAGE_SUFFIX_REGEX
import com.tencent.bkrepo.maven.constants.SNAPSHOT_BUILD_NUMBER
import com.tencent.bkrepo.maven.constants.SNAPSHOT_SUFFIX
import com.tencent.bkrepo.maven.constants.SNAPSHOT_TIMESTAMP
import com.tencent.bkrepo.maven.constants.X_CHECKSUM_SHA1
import com.tencent.bkrepo.maven.enum.HashType
import com.tencent.bkrepo.maven.enum.MavenMessageCode
import com.tencent.bkrepo.maven.enum.SnapshotBehaviorType
import com.tencent.bkrepo.maven.exception.ConflictException
import com.tencent.bkrepo.maven.exception.JarFormatException
import com.tencent.bkrepo.maven.exception.MavenArtifactNotFoundException
import com.tencent.bkrepo.maven.exception.MavenRequestForbiddenException
import com.tencent.bkrepo.maven.model.TMavenMetadataRecord
import com.tencent.bkrepo.maven.pojo.MavenBasicInfo
import com.tencent.bkrepo.maven.pojo.MavenArtifactVersionData
import com.tencent.bkrepo.maven.pojo.MavenGAVC
import com.tencent.bkrepo.maven.pojo.MavenMetadataSearchPojo
import com.tencent.bkrepo.maven.pojo.MavenRepoConf
import com.tencent.bkrepo.maven.pojo.response.MavenArtifactResponse
import com.tencent.bkrepo.maven.service.MavenExtService
import com.tencent.bkrepo.maven.service.MavenMetadataService
import com.tencent.bkrepo.maven.service.MavenOperationService
import com.tencent.bkrepo.maven.util.DigestUtils
import com.tencent.bkrepo.maven.util.JarUtils
import com.tencent.bkrepo.maven.util.MavenConfiguration.toMavenRepoConf
import com.tencent.bkrepo.maven.util.MavenConfiguration.versionBehaviorConflict
import com.tencent.bkrepo.maven.util.MavenGAVCUtils.mavenGAVC
import com.tencent.bkrepo.maven.util.MavenGAVCUtils.toMavenGAVC
import com.tencent.bkrepo.maven.util.MavenMetadataUtils
import com.tencent.bkrepo.maven.util.MavenMetadataUtils.reRender
import com.tencent.bkrepo.maven.util.MavenStringUtils.checksumType
import com.tencent.bkrepo.maven.util.MavenStringUtils.fileMimeType
import com.tencent.bkrepo.maven.util.MavenStringUtils.formatSeparator
import com.tencent.bkrepo.maven.util.MavenStringUtils.httpStatusCode
import com.tencent.bkrepo.maven.util.MavenStringUtils.isSnapshotMetadataChecksumUri
import com.tencent.bkrepo.maven.util.MavenStringUtils.isSnapshotMetadataUri
import com.tencent.bkrepo.maven.util.MavenStringUtils.isSnapshotNonUniqueUri
import com.tencent.bkrepo.maven.util.MavenStringUtils.isSnapshotUri
import com.tencent.bkrepo.maven.util.MavenStringUtils.resolverName
import com.tencent.bkrepo.maven.util.MavenUtil
import com.tencent.bkrepo.repository.api.MetadataClient
import com.tencent.bkrepo.repository.api.StageClient
import com.tencent.bkrepo.repository.api.VersionDependentsClient
import com.tencent.bkrepo.repository.constant.CoverStrategy
import com.tencent.bkrepo.repository.pojo.dependent.VersionDependentsRequest
import com.tencent.bkrepo.repository.pojo.download.PackageDownloadRecord
import com.tencent.bkrepo.repository.pojo.metadata.MetadataModel
import com.tencent.bkrepo.repository.pojo.metadata.MetadataSaveRequest
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import org.apache.commons.lang3.StringUtils
import org.apache.maven.artifact.repository.metadata.Metadata
import org.apache.maven.artifact.repository.metadata.Snapshot
import org.apache.maven.artifact.repository.metadata.SnapshotVersion
import org.apache.maven.artifact.repository.metadata.Versioning
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer
import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

@Component
class MavenLocalRepository(
    private val stageClient: StageClient,
    private val mavenMetadataService: MavenMetadataService,
    private val metadataClient: MetadataClient,
    private val versionDependentsClient: VersionDependentsClient,
    private val mavenExtService: MavenExtService,
    private val mavenOperationService: MavenOperationService
) : LocalRepository() {

    @Value("\${maven.domain:http://127.0.0.1:25803}")
    val mavenDomain = ""

    /**
     * 获取MAVEN节点创建请求
     */
    override fun buildNodeCreateRequest(context: ArtifactUploadContext): NodeCreateRequest {
        val request = super.buildNodeCreateRequest(context)
        val deploymentRepo = if (context.repositoryDetail.category == RepositoryCategory.VIRTUAL) {
            val repo = repositoryClient.getRepoDetail(context.projectId, context.repoName).data!!
            (repo.configuration as VirtualConfiguration).deploymentRepo.takeIf { !it.isNullOrEmpty() }
        } else null
        return request.copy(
            repoName = deploymentRepo ?: request.repoName,
            overwrite = true,
            nodeMetadata = createNodeMetaData(context.getArtifactFile())
        )
    }

    fun buildMavenArtifactNodeCreateRequest(context: ArtifactUploadContext): NodeCreateRequest {
        val request = super.buildNodeCreateRequest(context)
        // 此处对请求的fullPath 做处理
        val combineUrl = combineUrl(context, request.fullPath)
        logger.info(
            "Node store path is $combineUrl, and original path is ${context.artifactInfo.getArtifactFullPath()} " +
                "in repo ${context.artifactInfo.getRepoIdentify()}"
        )
        return request.copy(
            fullPath = combineUrl,
            overwrite = true,
            nodeMetadata = createNodeMetaData(context.getArtifactFile())
        )
    }

    private fun createNodeMetaData(artifactFile: ArtifactFile): List<MetadataModel> {
        with(artifactFile) {
            val md5 = getFileMd5()
            val sha1 = getFileSha1()
            val sha256 = getFileSha256()
            val sha512 = artifactFile.getInputStream().use {
                val bytes = it.readBytes()
                DigestUtils.sha512(bytes, 0, bytes.size)
            }
            return mutableMapOf(
                HashType.MD5.ext to md5,
                HashType.SHA1.ext to sha1,
                HashType.SHA256.ext to sha256,
                HashType.SHA512.ext to sha512
            ).map {
                MetadataModel(key = it.key, value = it.value)
            }.toMutableList()
        }
    }

    /**
     * 对请求参数和仓库SnapshotVersionBehavior的设置做判断，如果行为不一致生成新的url
     * 当[SnapshotBehaviorType.UNIQUE] 或 [SnapshotBehaviorType.NON_UNIQUE] 服务器才介入生成构件路径的过程
     */
    private fun combineUrl(
        context: ArtifactUploadContext,
        fullPath: String
    ): String {
        val mavenArtifactInfo = context.artifactInfo as MavenArtifactInfo
        logger.info(
            "Try to combine the url for ${mavenArtifactInfo.getArtifactFullPath()} " +
                "in repo ${mavenArtifactInfo.getRepoIdentify()}, and isSnapshot ${mavenArtifactInfo.isSnapshot()}"
        )
        val mavenRepoConf = getRepoConf(context)
        val snapshotFlag = mavenArtifactInfo.isSnapshot()
        if (!snapshotFlag) {
            return mavenArtifactInfo.getArtifactFullPath()
        }
        val name = fullPath.split("/").last()
        val result = when (mavenRepoConf.mavenSnapshotVersionBehavior) {
            SnapshotBehaviorType.NON_UNIQUE -> {
                val nonUniqueName = name.resolverName(
                    mavenArtifactInfo.artifactId,
                    mavenArtifactInfo.versionId
                ).combineToNonUnique()
                fullPath.replace(name, nonUniqueName)
            }
            SnapshotBehaviorType.UNIQUE -> {
                val mavenVersion = name.resolverName(mavenArtifactInfo.artifactId, mavenArtifactInfo.versionId)
                if (mavenVersion.timestamp.isNullOrBlank()) {
                    // 查询最新记录
                    mavenMetadataService.findAndModify(
                        MavenMetadataSearchPojo(
                            projectId = context.projectId,
                            repoName = context.repoName,
                            groupId = mavenArtifactInfo.groupId,
                            artifactId = mavenArtifactInfo.artifactId,
                            version = mavenArtifactInfo.versionId,
                            classifier = mavenVersion.classifier,
                            extension = mavenVersion.packaging
                        )
                    ).apply {
                        mavenVersion.timestamp = this.timestamp
                        mavenVersion.buildNo = this.buildNo
                    }
                    val nonUniqueName = mavenVersion.combineToUnique()
                    fullPath.replace(name, nonUniqueName)
                } else {
                    null
                }
            }
            else -> null
        }
        return result ?: mavenArtifactInfo.getArtifactFullPath()
    }

    /**
     *
     */
    private fun buildMavenArtifactNode(
        context: ArtifactUploadContext,
        packaging: String,
        mavenGavc: MavenGAVC
    ): NodeCreateRequest {
        val request = buildMavenArtifactNodeCreateRequest(context)
        val metadata = request.nodeMetadata as? MutableList
        metadata?.add(MetadataModel(key = METADATA_KEY_PACKAGING, value = packaging, system = true))
        metadata?.add(MetadataModel(key = METADATA_KEY_GROUP_ID, value = mavenGavc.groupId, system = true))
        metadata?.add(MetadataModel(key = METADATA_KEY_ARTIFACT_ID, value = mavenGavc.artifactId, system = true))
        metadata?.add(MetadataModel(key = METADATA_KEY_VERSION, value = mavenGavc.version, system = true))
        metadata?.add(MetadataModel(key = METADATA_KEY_MODEL_VERSION, value = mavenGavc.modelVersion ?: SLASH, system = true))
        metadata?.add(MetadataModel(key = METADATA_KEY_NAME, value = mavenGavc.name ?: SLASH, system = true))
        mavenGavc.classifier?.let { metadata?.add(MetadataModel(key = "classifier", value = it)) }
        return request
    }

    override fun coverStrategy(context: ArtifactUploadContext) {
        (context.artifactInfo as MavenArtifactInfo).let {
            val coverStrategy = context.repositoryDetail.coverStrategy
            logger.info("The repo[${it.repoName}] coverStrategy:$coverStrategy")
            if (it.isArtifact() && coverStrategy == CoverStrategy.UNCOVER) {
                val fullPath = it.getArtifactFullPath()
                nodeClient.getNodeDetail(
                    projectId = it.projectId,
                    repoName = it.repoName,
                    fullPath = fullPath
                ).data?.let { _ ->
                    throw MavenRequestForbiddenException(
                        MavenMessageCode.MAVEN_ARTIFACT_COVER_FORBIDDEN, fullPath, it.getRepoIdentify()
                    )
                }
            }
        }
    }

    override fun onUploadBefore(context: ArtifactUploadContext) {
        super.onUploadBefore(context)
        val noOverwrite = HeaderUtils.getBooleanHeader("X-BKREPO-NO-OVERWRITE")
        val path = context.artifactInfo.getArtifactFullPath()
        logger.info("The File $path does not want to be overwritten: $noOverwrite")
        val node = nodeClient.getNodeDetail(
            context.artifactInfo.projectId,
            context.artifactInfo.repoName,
            path
        ).data
        if (noOverwrite) {
            // -SNAPSHOT/** 路径下的metadata.xml 文件不做判断
            if (!path.endsWith(MAVEN_METADATA_FILE_NAME)) {
                if (node != null && path.checksumType() == null) {
                    val message = "The File $path already existed in the ${context.artifactInfo.getRepoIdentify()}, " +
                        "please check your overwrite configuration."
                    logger.warn(message)
                    throw MavenRequestForbiddenException(
                        MavenMessageCode.MAVEN_REQUEST_FORBIDDEN, path, context.artifactInfo.getRepoIdentify()
                    )
                }
            }
        }
        // TODO: 需要抽象处理
        node?.let {
            uploadIntercept(context, it)
            packageVersion(null, node)?.let { packageVersion -> uploadIntercept(context, packageVersion) }
        }
        for (hashType in HashType.values()) {
            val artifactFullPath = context.artifactInfo.getArtifactFullPath()
            val suffix = ".${hashType.ext}"
            val isDigestFile = artifactFullPath.endsWith(suffix)
            if (isDigestFile) {
                // 校验hash
                validateDigest(hashType, context)
                return
            }
        }
    }

    private fun validateDigest(
        hashType: HashType,
        context: ArtifactUploadContext
    ) {
        with(context) {
            val suffix = ".${hashType.ext}"
            val artifactFilePath = artifactInfo.getArtifactFullPath().removeSuffix(suffix)
            // *-SNAPSHOT/maven-metadata.xml 交由服务生成后，lastUpdated会与客户端生成的不同，导致后续checksum校验不通过
            // *-SNAPSHOT/*-SNAPSHOT.jar 的构件上传后，如果仓库设置为`unique` 服务端生成时间戳，无法找到对应节点
            val repoConf = getRepoConf(context)
            if (artifactFilePath.isSnapshotUri() &&
                (
                    artifactFilePath.endsWith(MAVEN_METADATA_FILE_NAME) ||
                        repoConf.versionBehaviorConflict(artifactFilePath)
                    )
            ) {
                return
            }
            val node =
                nodeClient.getNodeDetail(projectId, repoName, artifactFilePath).data ?: throw NotFoundException(
                    ArtifactMessageCode.NODE_NOT_FOUND,
                    artifactFilePath
                )
            val serverDigest = node.metadata[hashType.ext].toString()
            val clientDigest = MavenUtil.extractDigest(getArtifactFile().getInputStream())
            if (clientDigest != serverDigest) {
                throw ConflictException(MavenMessageCode.MAVEN_CHECKSUM_CONFLICT, clientDigest, serverDigest)
            }
        }
    }

    private fun getRepoConf(context: ArtifactContext): MavenRepoConf {
        return context.repositoryDetail.configuration.toMavenRepoConf()
    }

    override fun onUpload(context: ArtifactUploadContext) {
        val matcher = Pattern.compile(PACKAGE_SUFFIX_REGEX).matcher(context.artifactInfo.getArtifactFullPath())
        logger.info(
            "Handling request to upload file ${context.artifactInfo.getArtifactFullPath()} " +
                "in repo ${context.artifactInfo.getRepoIdentify()}"
        )
        if (matcher.matches()) {
            var packaging = matcher.group(2)
            logger.info("File's package is $packaging")

            val fileSuffix = packaging
            val model: Model? = if (packaging == "pom") {
                val mavenPomModel = context.getArtifactFile().getInputStream().use { MavenXpp3Reader().read(it) }
                val version = if (mavenPomModel.version == null) {
                    mavenPomModel.parent.version
                } else {
                    mavenPomModel.version
                }
                val verNotBlank = StringUtils.isNotBlank(version)
                val isPom = mavenPomModel.packaging.equals("pom", ignoreCase = true)
                if (!verNotBlank || !isPom) {
                    packaging = mavenPomModel.packaging
                }
                mavenPomModel
            } else {
                context.getArtifactFile().apply {
                    if (this.getFile() == null) this.flushToFile()
                }
                val file = context.getArtifactFile().getFile()!!
                // todo pom.xml 可能为空
                // reference: org.springframework:spring-webmvc:4.3.7.RELEASE
                try {
                    JarUtils.parseModelInJar(file)
                } catch (e: JarFormatException) {
                    null
                }
            }
            val isArtifact = (packaging == fileSuffix)
            logger.info("Current file is artifact: $isArtifact")
            val mavenGavc = (context.artifactInfo as MavenArtifactInfo).toMavenGAVC(model)
            val node = buildMavenArtifactNode(context, packaging, mavenGavc)
            storageManager.storeArtifactFile(
                request = node,
                artifactFile = context.getArtifactFile(),
                storageCredentials = context.storageCredentials
            )
            context.putAttribute(FULL_PATH, node.fullPath)
            if (isArtifact) {
                createMavenVersion(context, mavenGavc, node.fullPath)
                // 记录依赖信息
                model?.let { mavenExtService.addVersionDependents(mavenGavc, it, context.projectId, context.repoName) }
            }
            // 更新包各模块版本最新记录
            logger.info("Prepare to create maven metadata....")
            try {
                mavenMetadataService.update(node)
            } catch (e: DuplicateKeyException) {
                logger.warn(
                    "DuplicateKeyException occurred during updating metadata for " +
                        "${context.artifactInfo.getArtifactFullPath()} " +
                        "in repo ${context.artifactInfo.getRepoIdentify()}"
                )
            }
        } else {
            val artifactFullPath = context.artifactInfo.getArtifactFullPath()
            val isSnapShotUri = artifactFullPath.isSnapshotUri()
            val metadataCheckSum = metadataUploadHandler(artifactFullPath, context)
            val artifactCheckSum = artifactUploadHandler(artifactFullPath, context)
            logger.info(
                "Handling request to upload unmatched file $artifactFullPath " +
                    "in repo ${context.artifactInfo.getRepoIdentify()}"
            )
            // -SNAPSHOT/** 路径下的构件和metadata.xml 文件的checksum 做拦截，
            // metadata.xml.* 改由系统生成
            // 构件名如果与仓库配置不符也改由系统生成
            if (isSnapShotUri && (metadataCheckSum || artifactCheckSum)) {
                logger.info(
                    "The unmatched file $artifactFullPath will be generated by server side " +
                        "in repo ${context.artifactInfo.getRepoIdentify()}"
                )
                return
            } else {
                super.onUpload(context)
            }
        }
    }

    private fun metadataUploadHandler(artifactFullPath: String, context: ArtifactContext): Boolean {
        val repoConf = getRepoConf(context)
        if (repoConf.mavenSnapshotVersionBehavior != SnapshotBehaviorType.DEPLOYER) {
            for (hashType in HashType.values()) {
                val suffix = ".${hashType.ext}"
                val isDigestFile = artifactFullPath.endsWith(suffix)
                if (isDigestFile) {
                    val artifactFilePath = artifactFullPath.removeSuffix(suffix)
                    return artifactFilePath.endsWith(MAVEN_METADATA_FILE_NAME)
                }
            }
        }
        return false
    }

    /**
     * 当上传构件url 为 1.0-SNAPSHOT/xx-1.0-SNAPSHOT.jar
     * 仓库属性  SnapshotVersionBehavior == [SnapshotBehaviorType.UNIQUE]
     *
     */
    private fun artifactUploadHandler(artifactFullPath: String, context: ArtifactContext): Boolean {
        val repoConf = getRepoConf(context)
        return (
            artifactFullPath.checksumType() != null &&
                repoConf.versionBehaviorConflict(artifactFullPath)
            )
    }

    /**
     * 上传pom 和 jar 时返回文件上传成功信息
     */
    override fun onUploadSuccess(context: ArtifactUploadContext) {
        super.onUploadSuccess(context)
        val repoConf = getRepoConf(context)
        with(context) {
            val fullPath = context.getStringAttribute(FULL_PATH) ?: artifactInfo.getArtifactFullPath()
            val mimeType = fullPath.fileMimeType()
            if (mimeType != null) {
                val node =
                    nodeClient.getNodeDetail(projectId, repoName, fullPath).data ?: return
                val uri = "$mavenDomain/$projectId/$repoName/${node.fullPath}"
                val mavenArtifactResponse = MavenArtifactResponse(
                    projectId = node.projectId,
                    repo = node.repoName,
                    created = node.createdDate,
                    createdBy = node.createdBy,
                    downloadUri = uri,
                    mimeType = mimeType,
                    size = node.size.toString(),
                    checksums = MavenArtifactResponse.Checksums(
                        sha1 = node.metadata["sha1"] as? String,
                        md5 = node.md5,
                        sha256 = node.sha256
                    ),
                    originalChecksums = MavenArtifactResponse.OriginalChecksums(node.sha256),
                    uri = uri
                )
                response.status = fullPath.httpStatusCode(repoConf)
                response.writer.println(mavenArtifactResponse.toJsonString())
                response.writer.flush()
            }
        }
    }

    override fun onUploadFinished(context: ArtifactUploadContext) {
        super.onUploadFinished(context)
        val repoConf = getRepoConf(context)
        val artifactFullPath = context.getStringAttribute(FULL_PATH) ?: context.artifactInfo.getArtifactFullPath()
        val isSnapshotUri = artifactFullPath.isSnapshotUri()
        val isSnapshotNonUniqueUri = artifactFullPath.isSnapshotNonUniqueUri()
        logger.info(
            "The file $artifactFullPath has been uploaded, isSnapshotUri $isSnapshotUri " +
                "the isSnapshotNonUniqueUri is $isSnapshotNonUniqueUri," +
                "the snapshot behavior type is ${repoConf.mavenSnapshotVersionBehavior}"
        )

        if (!isSnapshotUri || repoConf.mavenSnapshotVersionBehavior == SnapshotBehaviorType.DEPLOYER) {
            return
        }
        // 生成`maven-metadata.xml`
        if (artifactFullPath.endsWith(MAVEN_METADATA_FILE_NAME)) {
            verifyMetadataContent(context)
            return
        }
        if (artifactFullPath.checksumType() == null) {
            // 处理maven2 *1.0-SNAPSHOT/1.0-SNAPSHOT.jar 格式构件
            // 对应 checksum 有客户端请求时再去生成，因为客户端 在上传时 不知道由服务器生成的 时间戳
            // 在.pom 上传完之后需要重新生成 maven-metadata.xml , 已记录由服务器生成的最新构件
            // 处理maven-metadata.xml文件上传顺序无法确定，导致metadata文件无法更新的问题
            verifyMetadataContent(context, artifactFullPath)
            verifyPath(context, artifactFullPath)
            return
        }
    }

    /**
     * [artifactPath] maven-metadata.xml 父文件夹
     * 在指定构件路径下，服务生成 快照版本下的maven-metadata.xml,
     */
    private fun verifyMetadataContent(context: ArtifactContext, artifactPath: String? = null) {
        val fullPath = context.artifactInfo.getArtifactFullPath()
        logger.info(
            "Handling request to create maven-metadata.xml for $fullPath " +
                "and with special path $artifactPath in repo ${context.artifactInfo.getRepoIdentify()}"
        )
        val mavenGavc = fullPath.mavenGAVC()
        val repoConf = getRepoConf(context)
        val records = mavenMetadataService.search(context.artifactInfo, mavenGavc)
        if (records.isEmpty()) return
        generateMetadata(repoConf.mavenSnapshotVersionBehavior, mavenGavc, records)?.let {
            reStoreMavenMetadataRelated(it, context, fullPath, artifactPath)
        }
    }

    /**
     * 存储新生成的maven-metadata.xml文件以及对应的md5，sha1等等
     */
    private fun reStoreMavenMetadataRelated(
        metadata: org.apache.maven.artifact.repository.metadata.Metadata,
        context: ArtifactContext,
        fullPath: String,
        artifactPath: String? = null
    ) {
        logger.info("$fullPath file's metadata has been generated, now will store it")
        ByteArrayOutputStream().use { bos ->
            MetadataXpp3Writer().write(bos, metadata)
            val artifactFile = ArtifactFileFactory.build(bos.toByteArray().inputStream())
            try {
                val path = if (artifactPath == null) {
                    fullPath
                } else {
                    "${artifactPath.substringBeforeLast('/')}/$MAVEN_METADATA_FILE_NAME"
                }
                val snapshotTimestamp = metadata.versioning?.snapshot?.timestamp?.replace(".", "")
                val snapshotBuildNumber = metadata.versioning?.snapshot?.buildNumber
                updateMetadata(context.repositoryDetail, path, artifactFile, snapshotTimestamp, snapshotBuildNumber)
                verifyPath(context, path)
            } finally {
                artifactFile.delete()
            }
        }
    }

    /**
     * 生成对应checksum文件
     */
    private fun verifyPath(context: ArtifactContext, fullPath: String, hashType: HashType? = null) {
        logger.info(
            "Will go to update checkSum files for $fullPath " +
                "in repo ${context.artifactInfo.getRepoIdentify()}"
        )
        val node = nodeClient.getNodeDetail(
            projectId = context.projectId,
            repoName = context.repoName,
            fullPath = fullPath
        ).data ?: return
        val typeArray = if (hashType == null) {
            HashType.values()
        } else {
            arrayOf(hashType)
        }
        updateArtifactCheckSum(context, node, typeArray)
    }

    /**
     * 上传后更新构件的checksum文件
     * 先尝试从文件元数据中取，如果没有则从节点数据或者服务端计算
     */
    private fun updateArtifactCheckSum(context: ArtifactContext, node: NodeDetail, typeArray: Array<HashType>) {
        logger.info(
            "Ready to generate checksum file on server side for ${node.fullPath} " +
                "in repo ${context.artifactInfo.getRepoIdentify()}"
        )
        for (hashType in typeArray) {
            val checksum = checksumNode(context, node, hashType)
            checksum?.let {
                generateChecksum(node, hashType, checksum, context.storageCredentials)
            }
        }
    }

    private fun checksumNode(context: ArtifactContext, node: NodeDetail, hashType: HashType): String? {
        val checksumValue = if ((node.metadata[hashType.ext] as? String)?.isNotBlank() == true) {
            return node.metadata[hashType.ext] as String
        } else if (hashType == HashType.MD5) {
            node.md5
        } else if (hashType == HashType.SHA256) {
            node.sha256
        } else if (hashType == HashType.SHA1) {
            storageManager.loadArtifactInputStream(node, context.storageCredentials)?.use {
                it.sha1()
            }
        } else if (hashType == HashType.SHA512) {
            storageManager.loadArtifactInputStream(node, context.storageCredentials)?.use {
                it.sha512()
            }
        } else {
            logger.error("Unsupported hash type $hashType: [${context.repositoryDetail} | ${context.artifactInfo}]")
            null
        }
        checksumValue?.let {
            logger.info("Save ${hashType.ext} checksum $it for [${context.repositoryDetail} | ${context.artifactInfo}]")
            metadataClient.saveMetadata(
                MetadataSaveRequest(
                    projectId = context.projectId,
                    repoName = context.repoName,
                    fullPath = node.fullPath,
                    nodeMetadata = listOf(MetadataModel(key= hashType.ext, value = it ,system = true, display = true))
                )
            )
        }
        return checksumValue
    }

    /**
     * 生成对应类型的checksum文件节点，并存储
     */
    private fun generateChecksum(
        node: NodeDetail,
        type: HashType,
        value: String,
        storageCredentials: StorageCredentials?
    ) {
        val artifactFile = ArtifactFileFactory.build(value.byteInputStream())
        try {
            val nodeCreateRequest = NodeCreateRequest(
                projectId = node.projectId,
                repoName = node.repoName,
                fullPath = "${node.fullPath}.${type.ext}",
                folder = false,
                overwrite = true,
                size = artifactFile.getSize(),
                md5 = artifactFile.getFileMd5(),
                sha256 = artifactFile.getFileSha256(),
                operator = SecurityUtils.getUserId()
            )
            storageManager.storeArtifactFile(nodeCreateRequest, artifactFile, storageCredentials)
        } finally {
            artifactFile.delete()
        }
    }

    private fun generateMetadata(
        behaviorType: SnapshotBehaviorType?,
        mavenGavc: MavenGAVC,
        records: List<TMavenMetadataRecord>
    ): org.apache.maven.artifact.repository.metadata.Metadata? {
        logger.info("Starting to create maven metadata and behavior type is $behaviorType...")
        val pom = records.firstOrNull { it.extension == "pom" } ?: return null
        val pomLastUpdated = (pom.timestamp ?: ZonedDateTime.now(ZoneId.of("UTC")).format(formatter))
            .replace(".", "")
        return when (behaviorType) {
            SnapshotBehaviorType.NON_UNIQUE -> {
                buildMetadata(mavenGavc, pomLastUpdated)
            }
            else -> {
                val buildNo = if (pom.buildNo == 0) 1 else pom.buildNo
                buildMetadata(mavenGavc, pomLastUpdated, buildNo, pom.timestamp, records)
            }
        }
    }

    /**
     * 生成maven-metadata.xml中的所有属性
     */
    private fun buildMetadata(
        mavenGavc: MavenGAVC,
        pomLastUpdated: String,
        buildNo: Int = 1,
        pomTimestamp: String? = null,
        records: List<TMavenMetadataRecord>? = null
    ): org.apache.maven.artifact.repository.metadata.Metadata {
        return org.apache.maven.artifact.repository.metadata.Metadata().apply {
            modelVersion = "1.1.0"
            groupId = mavenGavc.groupId
            artifactId = mavenGavc.artifactId
            version = mavenGavc.version
            versioning = generateVersioning(mavenGavc, pomLastUpdated, buildNo, pomTimestamp, records)
        }
    }

    /**
     * 生成maven-metadata.xml中的versioning属性
     */
    private fun generateVersioning(
        mavenGavc: MavenGAVC,
        pomLastUpdated: String,
        buildNo: Int = 1,
        pomTimestamp: String? = null,
        records: List<TMavenMetadataRecord>? = null
    ): Versioning {
        return Versioning().apply {
            snapshot = Snapshot().apply {
                pomTimestamp?.let {
                    timestamp = pomTimestamp
                }
                buildNumber = buildNo
            }
            lastUpdated = pomLastUpdated
            records?.let {
                snapshotVersions = generateSnapshotVersions(mavenGavc, records)
            }
        }
    }

    /**
     * 生成maven-metadata.xml中的snapshotVersions属性
     */
    private fun generateSnapshotVersions(
        mavenGavc: MavenGAVC,
        records: List<TMavenMetadataRecord>
    ): List<SnapshotVersion> {
        val snapshotVersionList = mutableListOf<SnapshotVersion>()
        for (record in records) {
            snapshotVersionList.add(
                SnapshotVersion().apply {
                    classifier = record.classifier
                    extension = record.extension
                    version = "${mavenGavc.version.removeSuffix(SNAPSHOT_SUFFIX)}-" +
                        "${record.timestamp}-${record.buildNo}"
                    updated = record.timestamp
                }
            )
        }
        return snapshotVersionList
    }

    /**
     * checksum 文件不存在时，系统生成
     */
    override fun onDownload(context: ArtifactDownloadContext): ArtifactResource? {
        with(context) {
            getFullPathInterceptors().forEach { it.intercept(projectId, artifactInfo.getArtifactFullPath()) }
            val node = getNodeInfoForDownload(context) ?: return null
            downloadIntercept(context, node)
            node.nodeMetadata.find { it.key == HashType.SHA1.ext }?.let {
                response.addHeader(X_CHECKSUM_SHA1, it.value.toString())
            }
            if (node.fullPath.isSnapshotMetadataUri() || node.fullPath.isSnapshotMetadataChecksumUri()) {
                val metadataNode = if (node.fullPath.checksumType() == null) node else {
                    val metadataFullPath = artifactInfo.getArtifactFullPath().substringBeforeLast(".")
                    nodeClient.getNodeDetail(projectId, repoName, metadataFullPath).data
                }
                val timestamp = metadataNode?.nodeMetadata?.find { it.key == SNAPSHOT_TIMESTAMP }?.value
                    ?: try {
                        MetadataXpp3Reader().read(storageManager.loadArtifactInputStream(node, storageCredentials))
                            ?.versioning?.snapshot?.timestamp?.replace(".", "")
                    } catch (ignore: Exception) {
                        null
                    }
                timestamp?.let { context.putAttribute(SNAPSHOT_TIMESTAMP, it) }
            }
            val inputStream = storageManager.loadArtifactInputStream(node, storageCredentials) ?: return null
            val responseName = artifactInfo.getResponseName()
            val srcRepo = RepositoryIdentify(projectId, repoName)
            return ArtifactResource(inputStream, responseName, srcRepo, node, ArtifactChannel.LOCAL, useDisposition)
        }
    }

    override fun packageVersion(context: ArtifactContext?, node: NodeDetail?): PackageVersion? {
        requireNotNull(node)
        return mavenOperationService.packageVersion(node)
    }

    /**
     * 针对两种路径获取对应节点信息
     */
    private fun getNodeInfoForDownload(context: ArtifactDownloadContext): NodeDetail? {
        with(context) {
            var fullPath = artifactInfo.getArtifactFullPath()
            val checksumType = fullPath.checksumType()
            logger.info("Will download node $fullPath in repo ${artifactInfo.getRepoIdentify()}")
            // 针对正常路径
            var node = findNode(context, fullPath, checksumType)
            if (node != null) return node
            // 剔除文件夹路径
            if (checksumType == null) {
                if (!fullPath.matches(Regex(PACKAGE_SUFFIX_REGEX))) {
                    throw MavenArtifactNotFoundException(
                        MavenMessageCode.MAVEN_ARTIFACT_NOT_FOUND, fullPath, artifactInfo.getRepoIdentify()
                    )
                }
            }
            // maven-metadata.xml
            if (fullPath.endsWith("$MAVEN_METADATA_FILE_NAME.${checksumType?.ext}")) return null
            // 剔除类似-20190917.073536-2.jar路径，实际不存在的
            if (!fullPath.isSnapshotNonUniqueUri()) {
                throw MavenArtifactNotFoundException(
                    MavenMessageCode.MAVEN_ARTIFACT_NOT_FOUND, fullPath, artifactInfo.getRepoIdentify()
                )
            }
            val mavenArtifactInfo = context.artifactInfo as MavenArtifactInfo
            try {
                mavenArtifactInfo.isSnapshot()
            } catch (e: Exception) {
                val maven = if (checksumType != null) {
                    fullPath.removeSuffix(".${checksumType.ext}").toMavenGAVC()
                } else {
                    fullPath.toMavenGAVC()
                }
                mavenArtifactInfo.artifactId = maven.artifactId
                mavenArtifactInfo.versionId = maven.version
                mavenArtifactInfo.groupId = maven.groupId
            }
            if (isUniqueAndSnapshot(context, mavenArtifactInfo)) {
                // 针对非正常路径: 获取后缀为-1.0.0-SNAPSHOT.jar， 实际存储后缀为-1.0.0-20190917.073536-2.jar
                fullPath = urlConvert(context, checksumType, mavenArtifactInfo) ?: return null
                logger.info(
                    "Will download node ${artifactInfo.getArtifactFullPath()} " +
                        "with the new path $fullPath in repo ${artifactInfo.getRepoIdentify()}"
                )
                node = findNode(context, fullPath, checksumType)
            }
            return node
        }
    }

    /**
     * 针对可能源文件存在，但是实际上对应checksum文件不存在需要处理
     */
    private fun findNode(
        context: ArtifactDownloadContext,
        fullPath: String,
        checksumType: HashType? = null
    ): NodeDetail? {
        with(context) {
            artifactInfo.setArtifactMappingUri(fullPath)
            var node = ArtifactContextHolder.getNodeDetail(artifactInfo)
            if (node != null || checksumType == null) {
                return node
            }
            // 针对可能文件上传，但是对应checksum文件没有生成的情况
            logger.info(
                "Will try to get $fullPath after removing suffix ${checksumType.ext} " +
                    "in ${artifactInfo.getRepoIdentify()}"
            )
            val temPath = fullPath.removeSuffix(".${checksumType.ext}")
            artifactInfo.setArtifactMappingUri(temPath)
            node = ArtifactContextHolder.getNodeDetail(artifactInfo)
            // 源文件存在，但是对应checksum文件不存在，需要生成
            if (node != null) {
                verifyPath(context, temPath, checksumType)
                artifactInfo.setArtifactMappingUri(fullPath)
                node = ArtifactContextHolder.getNodeDetail(artifactInfo)
            }
            return node
        }
    }

    /**
     * 判断是否是snapshot并且仓库是UNIQUE
     */
    private fun isUniqueAndSnapshot(context: ArtifactDownloadContext, mavenArtifactInfo: MavenArtifactInfo): Boolean {
        val mavenRepoConf = getRepoConf(context)
        val snapshotFlag = mavenArtifactInfo.isSnapshot()
        if (!snapshotFlag) {
            return false
        }
        if (mavenRepoConf.mavenSnapshotVersionBehavior != SnapshotBehaviorType.UNIQUE) {
            return false
        }
        return true
    }

    /**
     * 针对unique仓库，snapshot版本，直接下载，导致路径不存在的问题特殊处理
     * 源请求： jungle-udp-l5/1.0.0-SNAPSHOT/jungle-udp-l5-1.0.0-SNAPSHOT.jar
     * 实际存储路径： jungle-udp-l5/1.0.0-SNAPSHOT/jungle-udp-l5-1.0.0-20190917.073536-2.jar
     */
    private fun urlConvert(
        context: ArtifactDownloadContext,
        checksumType: HashType? = null,
        mavenArtifactInfo: MavenArtifactInfo
    ): String? {
        val fullPath = if (checksumType == null) {
            mavenArtifactInfo.getArtifactFullPath()
        } else {
            mavenArtifactInfo.getArtifactFullPath().removeSuffix(".${checksumType.ext}")
        }
        val name = fullPath.split("/").last()
        logger.info("The name of fullPath $fullPath is $name in repo ${context.artifactInfo.getRepoIdentify()}")
        val path = getUniquePath(
            fullPath = fullPath,
            name = name,
            mavenArtifactInfo = mavenArtifactInfo,
            projectId = context.projectId,
            repoName = context.repoName
        ) ?: return null
        return if (checksumType == null) {
            path
        } else {
            path + ".${checksumType.ext}"
        }
    }

    /**
     * 将-1.0.0-SNAPSHOT.jar转换为1.0.0-20190917.073536-2.jar
     */
    private fun getUniquePath(
        fullPath: String,
        name: String,
        mavenArtifactInfo: MavenArtifactInfo,
        projectId: String,
        repoName: String
    ): String? {
        val mavenVersion = name.resolverName(mavenArtifactInfo.artifactId, mavenArtifactInfo.versionId)
        val list = mavenMetadataService.search(
            MavenMetadataSearchPojo(
                projectId = projectId,
                repoName = repoName,
                groupId = mavenArtifactInfo.groupId,
                artifactId = mavenArtifactInfo.artifactId,
                version = mavenArtifactInfo.versionId,
                classifier = mavenVersion.classifier,
                extension = mavenVersion.packaging
            )
        )
        if (list.isNullOrEmpty()) return null
        list[0].apply {
            mavenVersion.timestamp = this.timestamp
            mavenVersion.buildNo = this.buildNo
        }
        val uniqueName = mavenVersion.combineToUnique()
        val nonUniqueName = mavenVersion.combineToNonUnique()
        // 针对非正常路径： 获取后缀为-1.0.0-SNAPSHOT.jar， 但是版本和versionId不一致的
        if (nonUniqueName != name) {
            throw MavenArtifactNotFoundException(
                MavenMessageCode.MAVEN_ARTIFACT_NOT_FOUND, fullPath, "$projectId|$repoName"
            )
        }
        return fullPath.replace(name, uniqueName)
    }

    private fun createMavenVersion(context: ArtifactUploadContext, mavenGAVC: MavenGAVC, fullPath: String) {
        val metadata = mutableListOf(
            MetadataModel(key = METADATA_KEY_PACKAGING, value = mavenGAVC.packaging, system = true),
            MetadataModel(key = METADATA_KEY_GROUP_ID, value = mavenGAVC.groupId, system = true),
            MetadataModel(key = METADATA_KEY_ARTIFACT_ID, value = mavenGAVC.artifactId, system = true),
            MetadataModel(key = METADATA_KEY_VERSION, value = mavenGAVC.version, system = true)
        )
        mavenGAVC.name?.let { metadata.add(MetadataModel(key = METADATA_KEY_NAME, value = it)) }
        mavenGAVC.modelVersion?.let{ metadata.add(MetadataModel(key = METADATA_KEY_MODEL_VERSION, value = it)) }
        mavenGAVC.classifier?.let { metadata.add(MetadataModel(key = METADATA_KEY_CLASSIFIER, value = it)) }
        try {
            packageClient.createVersion(
                PackageVersionCreateRequest(
                    context.projectId,
                    context.repoName,
                    packageName = mavenGAVC.artifactId,
                    packageKey = PackageKeys.ofGav(mavenGAVC.groupId, mavenGAVC.artifactId),
                    packageType = if (context.repositoryDetail.type == RepositoryType.GRADLE) PackageType.GRADLE else PackageType.MAVEN,
                    versionName = mavenGAVC.version,
                    size = context.getArtifactFile().getSize(),
                    artifactPath = fullPath,
                    overwrite = true,
                    createdBy = context.userId,
                    packageMetadata = metadata
                )
            )
        } catch (ignore: DuplicateKeyException) {
            logger.warn(
                "The package info has been created for version[${mavenGAVC.version}] " +
                    "and package[${mavenGAVC.artifactId}] in repo ${context.artifactInfo.getRepoIdentify()}"
            )
        }
    }

    fun updateMetadata(
        repositoryDetail: RepositoryDetail,
        fullPath: String,
        metadataArtifact: ArtifactFile,
        snapshotTimestamp: String? = null,
        snapshotBuildNumber: Int? = null
    ) {
        val metadataList = createNodeMetaData(metadataArtifact).toMutableList()
        if (fullPath.isSnapshotMetadataUri()) {
            snapshotTimestamp?.let { metadataList.add(MetadataModel(key = SNAPSHOT_TIMESTAMP, value = it)) }
            snapshotBuildNumber?.let { metadataList.add(MetadataModel(key = SNAPSHOT_BUILD_NUMBER, value = it)) }
        }
        val nodeCreateRequest = NodeCreateRequest(
            projectId = repositoryDetail.projectId,
            repoName = repositoryDetail.name,
            folder = false,
            fullPath = fullPath,
            size = metadataArtifact.getSize(),
            sha256 = metadataArtifact.getFileSha256(),
            md5 = metadataArtifact.getFileMd5(),
            operator = SecurityUtils.getUserId(),
            overwrite = true,
            nodeMetadata = metadataList
        )
        storageManager.storeArtifactFile(nodeCreateRequest, metadataArtifact, repositoryDetail.storageCredentials)
        metadataArtifact.delete()
        logger.info("Success to save $fullPath, size: ${metadataArtifact.getSize()}")
    }

    /**
     * 删除文件，删除对应文件后还需要更新对应的maven-metadata.xml文件, 同时还需要删除对应的metadata记录
     */
    override fun remove(context: ArtifactRemoveContext) {
        when (context.artifactInfo) {
            is MavenDeleteArtifactInfo -> {
                deletePackage(context)
            }
            else -> {
                deleteNode(context)
                // 更新对应的metadata文件
                verifyMetadataContent(context, context.artifactInfo.getArtifactFullPath())
            }
        }
    }

    /**
     * 删除package
     */
    fun deletePackage(context: ArtifactRemoveContext) {
        with(context.artifactInfo as MavenDeleteArtifactInfo) {
            logger.info("Will prepare to delete package [$packageName|$version] in repo ${getRepoIdentify()}")
            if (version.isBlank()) {
                packageClient.listAllVersion(projectId, repoName, packageName).data.orEmpty().forEach {
                    removeVersion(context, it)
                }
                // 删除package下得metadata.xml文件
                deleteNode(context, true)
            } else {
                packageClient.findVersionByName(projectId, repoName, packageName, version).data?.let {
                    removeVersion(context, it)
                    // 需要更新对应的metadata.xml文件
                    updatePackageMetadata(
                        context = context,
                        version = version
                    )
                } ?: throw VersionNotFoundException(version)
            }
        }
    }

    /**
     * 删除[version] 对应的node节点也会一起删除
     */
    private fun removeVersion(context: ArtifactRemoveContext, version: PackageVersion) {
        with(context.artifactInfo as MavenDeleteArtifactInfo) {
            logger.info(
                "Will delete package $packageName version ${version.name} in repo ${getRepoIdentify()}"
            )
            packageClient.deleteVersion(projectId, repoName, packageName, version.name)
            // 删除版本对应的依赖关系记录
            versionDependentsClient.delete(
                VersionDependentsRequest(
                    projectId = projectId,
                    repoName = repoName,
                    packageKey = packageName,
                    version = version.name
                )
            )
            val artifactPath = MavenUtil.extractPath(packageName) + "/${version.name}"
            // 需要删除对应的metadata表记录
            val (artifactId, groupId) = MavenUtil.extractGrounpIdAndArtifactId(packageName)
            val mavenGAVC = MavenGAVC(groupId=groupId, artifactId = artifactId, version = version.name, classifier = null)
            mavenMetadataService.delete(context.artifactInfo, null, mavenGAVC)
            val request = NodeDeleteRequest(
                projectId = projectId,
                repoName = repoName,
                fullPath = artifactPath,
                operator = context.userId
            )
            nodeClient.deleteNode(request)
        }
    }

    /**
     * 删除单个节点
     */
    private fun deleteNode(context: ArtifactRemoveContext, forceDeleted: Boolean = false) {
        with(context) {
            val fullPath = artifactInfo.getArtifactFullPath()
            logger.info("Will prepare to delete file $fullPath in repo ${artifactInfo.getRepoIdentify()} ")
            // 如果删除单个文件不能删除metadata.xml文件, 如果文件删除了对应的校验文件也要删除
            if (fullPath.endsWith(MAVEN_METADATA_FILE_NAME) && !forceDeleted) {
                throw MavenRequestForbiddenException(MavenMessageCode.MAVEN_ARTIFACT_DELETE_FORBIDDEN, fullPath)
            }
            val node = nodeClient.getNodeDetail(projectId, repoName, fullPath).data
            if (node != null) {
                if (node.fullPath.checksumType() == null) {
                    deleteArtifactCheckSums(
                        projectId = projectId,
                        repoName = repoName,
                        userId = userId,
                        node = node
                    )
                }
                // 需要删除对应的metadata表记录
                mavenMetadataService.delete(context.artifactInfo, node)
                val request = NodeDeleteRequest(
                    projectId = projectId,
                    repoName = repoName,
                    fullPath = fullPath,
                    operator = userId
                )
                nodeClient.deleteNode(request)
            } else {
                // 同样尝试删除对应的metadata表记录
                mavenMetadataService.delete(context.artifactInfo, node)
                // 抛异常改为error日志
                logger.error("Can not find node $fullPath in repo $artifactInfo")
            }
        }
    }

    /**
     * 删除构件的checksum文件
     */
    private fun deleteArtifactCheckSums(
        projectId: String,
        repoName: String,
        userId: String,
        node: NodeDetail,
        typeArray: Array<HashType> = HashType.values()
    ) {
        for (hashType in typeArray) {
            val fullPath = "${node.fullPath}.${hashType.ext}"
            nodeClient.getNodeDetail(projectId, repoName, fullPath).data?.let {
                val request = NodeDeleteRequest(
                    projectId = projectId,
                    repoName = repoName,
                    fullPath = fullPath,
                    operator = userId
                )
                nodeClient.deleteNode(request)
            }
        }
    }

    /**
     *  更新`/groupId/artifactId/maven-metadata.xml`文件
     */
    private fun updatePackageMetadata(
        context: ArtifactRemoveContext,
        version: String
    ) {
        // reference https://maven.apache.org/guides/getting-started/#what-is-a-snapshot-version
        // 查找 `/groupId/artifactId/maven-metadata.xml`
        with(context) {
            val node = nodeClient.getNodeDetail(projectId, repoName, artifactInfo.getArtifactFullPath()).data ?: return
            storageService.load(
                node.sha256!!,
                Range.full(node.size),
                storageCredentials
            ).use { artifactInputStream ->
                // 更新 `/groupId/artifactId/maven-metadata.xml`
                val mavenMetadata = MetadataXpp3Reader().read(artifactInputStream)
                mavenMetadata.versioning.versions.remove(version)
                if (mavenMetadata.versioning.versions.size == 0) {
                    nodeClient.deleteNode(NodeDeleteRequest(projectId, repoName, node.fullPath, userId))
                    deleteArtifactCheckSums(
                        projectId = projectId,
                        repoName = repoName,
                        userId = userId,
                        node = node
                    )
                    return
                }
                mavenMetadata.reRender()
                storeMetadataXml(context.repositoryDetail, mavenMetadata, node.path)
            }
        }
    }

    private fun storeMetadataXml(
        repoDetail: RepositoryDetail,
        mavenMetadata: Metadata,
        nodePath: String
    ) {
        val path = nodePath.ensureSuffix(SLASH)
        val storageCredentials = repoDetail.storageCredentials
        ByteArrayOutputStream().use { metadata ->
            MetadataXpp3Writer().write(metadata, mavenMetadata)
            val artifactFile = ArtifactFileFactory.build(metadata.toByteArray().inputStream(), storageCredentials)
            val resultXmlMd5 = artifactFile.getFileMd5()
            val resultXmlSha1 = artifactFile.getFileSha1()
            val metadataArtifactMd5 = ByteArrayInputStream(resultXmlMd5.toByteArray()).use {
                ArtifactFileFactory.build(it, storageCredentials)
            }
            val metadataArtifactSha1 = ByteArrayInputStream(resultXmlSha1.toByteArray()).use {
                ArtifactFileFactory.build(it, storageCredentials)
            }
            updateMetadata(repoDetail, "$path$MAVEN_METADATA_FILE_NAME", artifactFile)
            artifactFile.delete()
            updateMetadata(repoDetail, "$path$MAVEN_METADATA_FILE_NAME.${HashType.MD5.ext}", metadataArtifactMd5)
            metadataArtifactMd5.delete()
            updateMetadata(repoDetail, "$path$MAVEN_METADATA_FILE_NAME.${HashType.SHA1.ext}", metadataArtifactSha1)
            metadataArtifactSha1.delete()
        }
    }

    override fun query(context: ArtifactQueryContext): MavenArtifactVersionData {
        val packageKey = context.request.getParameter("packageKey")
        val version = context.request.getParameter("version")
        val artifactId = packageKey.split(":").last()
        val groupId = packageKey.removePrefix("gav://").split(":")[0]
        val trueVersion = packageClient.findVersionByName(
            context.projectId,
            context.repoName,
            packageKey,
            version
        ).data ?: throw MavenArtifactNotFoundException(
            MavenMessageCode.MAVEN_VERSION_NOT_FOUND, packageKey, version, "${context.projectId}/${context.repoName}"
        )
        with(context.artifactInfo) {
            val jarNode = nodeClient.getNodeDetail(
                projectId,
                repoName,
                trueVersion.contentPath!!
            ).data ?: throw NodeNotFoundException("${trueVersion.contentPath}")
            val type = (jarNode.metadata["packaging"] as? String) ?: "jar"
            val classifier = jarNode.metadata["classifier"] as? String
            val stageTag = stageClient.query(projectId, repoName, packageKey, version).data
            val packageVersion = packageClient.findVersionByName(
                projectId,
                repoName,
                packageKey,
                version
            ).data
            val count = packageVersion?.downloads ?: 0
            val createdDate = packageVersion?.createdDate?.format(DateTimeFormatter.ISO_DATE_TIME)
                ?: jarNode.createdDate
            val lastModifiedDate = packageVersion?.lastModifiedDate?.format(DateTimeFormatter.ISO_DATE_TIME)
                ?: jarNode.lastModifiedDate
            val mavenArtifactMavenBasicInfo = MavenBasicInfo(
                groupId,
                artifactId,
                version,
                classifier,
                if (type == "jar") null else type,
                jarNode.size, jarNode.fullPath,
                jarNode.createdBy, createdDate,
                jarNode.lastModifiedBy, lastModifiedDate,
                count,
                jarNode.sha256!!,
                jarNode.md5!!,
                projectId,
                repoName,
                stageTag ?: emptyList(),
                null
            )
            return MavenArtifactVersionData(mavenArtifactMavenBasicInfo, packageVersion?.packageMetadata)
        }
    }

    // maven 客户端下载统计
    override fun buildDownloadRecord(
        context: ArtifactDownloadContext,
        artifactResource: ArtifactResource
    ): PackageDownloadRecord? {
        with(context) {
            val fullPath = artifactInfo.getArtifactFullPath()
            val node = nodeClient.getNodeDetail(projectId, repoName, fullPath).data ?: return null
            val packaging = node.metadata[METADATA_KEY_PACKAGING] ?: return null
            if (node.name.endsWith(".pom") && packaging != "pom") {
                return null
            }
            val mavenGAVC = fullPath.mavenGAVC()
            val version = mavenGAVC.version
            val artifactId = mavenGAVC.artifactId
            val groupId = mavenGAVC.groupId.formatSeparator("/", ".")
            val packageKey = PackageKeys.ofGav(groupId, artifactId)
            return PackageDownloadRecord(projectId, repoName, packageKey, version, userId)
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
        return listOf(artifactPath.substringBeforeLast('/'))
    }

    // TODO("依赖分析数据更新")
    override fun updateIndex(projectId: String, repoName: String, key: String, version: String) {
        val repoDetail = repositoryClient.getRepoDetail(projectId, repoName).data!!
        val storageCredentials = repoDetail.storageCredentials
        if (packageClient.findPackageByKey(projectId, repoName, key).data != null) {
            val metadataFullPath = MavenUtil.extractPath(key) + "/$MAVEN_METADATA_FILE_NAME"
            // 目标maven制品的maven-metadata.xml文件可能不存在
            val node = nodeClient.getNodeDetail(projectId, repoName, metadataFullPath).data
                ?: run {
                    logger.error(
                        "Failed to update index for [$projectId/$repoName/$key] because of missing metadata file"
                    )
                    return
                }
            storageService.load(node.sha256!!, Range.full(node.size), storageCredentials).use { artifactInputStream ->
                val mavenMetadata = MetadataXpp3Reader().read(artifactInputStream)
                if (!mavenMetadata.versioning.versions.contains(version)) {
                    mavenMetadata.versioning.versions.add(version)
                }
                mavenMetadata.reRender()
                storeMetadataXml(repoDetail, mavenMetadata, node.path)
            }
        } else {
            val (artifactId, groupId) = MavenUtil.extractGrounpIdAndArtifactId(key)
            val metadata = MavenMetadataUtils.initMetadataByGav(groupId, artifactId, version)
            storeMetadataXml(repoDetail, metadata, MavenUtil.extractPath(key))
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MavenLocalRepository::class.java)
        private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd.HHmmss")
    }
}

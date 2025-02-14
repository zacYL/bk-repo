/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.maven.service.impl

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.constant.PARAM_DOWNLOAD
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.core.ArtifactExtService
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.view.ViewModelService
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.maven.artifact.MavenArtifactInfo
import com.tencent.bkrepo.maven.artifact.MavenDeleteArtifactInfo
import com.tencent.bkrepo.maven.constants.MAVEN_METADATA_FILE_NAME
import com.tencent.bkrepo.maven.enum.MavenMessageCode
import com.tencent.bkrepo.maven.exception.JarFormatException
import com.tencent.bkrepo.maven.exception.MavenArtifactFormatException
import com.tencent.bkrepo.maven.exception.MavenBadRequestException
import com.tencent.bkrepo.maven.pojo.MavenArtifactVersionData
import com.tencent.bkrepo.maven.pojo.MavenMetadataSearchPojo
import com.tencent.bkrepo.maven.pojo.MavenVersion
import com.tencent.bkrepo.maven.pojo.request.MavenWebDeployRequest
import com.tencent.bkrepo.maven.pojo.response.MavenWebDeployResponse
import com.tencent.bkrepo.maven.service.MavenMetadataService
import com.tencent.bkrepo.maven.service.MavenService
import com.tencent.bkrepo.maven.util.JarUtils
import com.tencent.bkrepo.maven.util.JarUtils.readModel
import com.tencent.bkrepo.maven.util.MavenMetadataUtils.initByModel
import com.tencent.bkrepo.maven.util.MavenMetadataUtils.reRender
import com.tencent.bkrepo.maven.util.MavenModelUtils.toArtifact
import com.tencent.bkrepo.maven.util.MavenModelUtils.toArtifactUri
import com.tencent.bkrepo.maven.util.MavenModelUtils.toMetadataUri
import com.tencent.bkrepo.maven.util.MavenModelUtils.toPom
import com.tencent.bkrepo.maven.util.MavenModelUtils.toPomUri
import com.tencent.bkrepo.maven.util.MavenModelUtils.toSnapshotArtifactUri
import com.tencent.bkrepo.maven.util.MavenModelUtils.toSnapshotMetadataUri
import com.tencent.bkrepo.maven.util.MavenModelUtils.toSnapshotPomUri
import com.tencent.bkrepo.maven.util.MavenStringUtils.isSnapshotUri
import com.tencent.bkrepo.maven.util.MavenStringUtils.resolverName
import com.tencent.bkrepo.maven.util.MavenUtil
import com.tencent.bkrepo.repository.pojo.list.HeaderItem
import com.tencent.bkrepo.repository.pojo.list.RowItem
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.NodeListOption
import com.tencent.bkrepo.repository.pojo.node.NodeListViewItem
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import org.apache.maven.artifact.repository.metadata.Metadata
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer
import org.apache.maven.model.Model
import org.apache.maven.model.Profile.SOURCE_POM
import org.apache.maven.model.io.xpp3.MavenXpp3Writer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.PatternSyntaxException

@Service
class MavenServiceImpl(
    private val viewModelService: ViewModelService,
    private val storageManager: StorageManager,
    private val storageService: StorageService,
    private val mavenMetadataService: MavenMetadataService
) : ArtifactExtService(), MavenService {

    @Value("\${spring.application.name}")
    private var applicationName: String = "maven"

    @Permission(type = ResourceType.REPO, action = PermissionAction.WRITE)
    override fun deploy(
        mavenArtifactInfo: MavenArtifactInfo,
        file: ArtifactFile
    ) {
        val context = ArtifactUploadContext(file)
        try {
            ArtifactContextHolder.getRepository().upload(context)
        } catch (e: PatternSyntaxException) {
            logger.warn(
                "Error [${e.message}] occurred during uploading ${mavenArtifactInfo.getArtifactFullPath()} " +
                    "in repo ${mavenArtifactInfo.getRepoIdentify()}"
            )
            throw MavenBadRequestException(
                MavenMessageCode.MAVEN_ARTIFACT_UPLOAD, mavenArtifactInfo.getArtifactFullPath()
            )
        }
    }

    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    override fun dependency(mavenArtifactInfo: MavenArtifactInfo) {
        // 为了兼容jfrog，当查询到目录时，会展示当前目录下所有子项，而不是直接报错
        with(mavenArtifactInfo) {
            val node = nodeService.getNodeDetail(mavenArtifactInfo)
            val download = HttpContextHolder.getRequest().getParameter(PARAM_DOWNLOAD)?.toBoolean() ?: false
            if (node != null) {
                if (node.folder && !download) {
                    logger.info("The folder: ${getArtifactFullPath()} will be displayed...")
                    renderListView(node, this)
                } else {
                    logger.info("The dependency file: ${getArtifactFullPath()} will be downloaded... ")
                    val context = ArtifactDownloadContext()
                    ArtifactContextHolder.getRepository().download(context)
                }
            } else {
                logger.info("The dependency file: ${getArtifactFullPath()} will be downloaded... ")
                val context = ArtifactDownloadContext()
                ArtifactContextHolder.getRepository().download(context)
            }
        }
    }

    /**
     * 当查询节点为目录时，将其子节点以页面形式展示
     */
    private fun renderListView(node: NodeDetail, artifactInfo: MavenArtifactInfo) {
        with(artifactInfo) {
            viewModelService.trailingSlash(applicationName)
            // listNodePage 接口没办法满足当前情况
            val nodeList = nodeService.listNode(
                this, NodeListOption(includeFolder = true, deep = false)
            )
            val currentPath = viewModelService.computeCurrentPath(node)
            val headerList = listOf(
                HeaderItem("Name"),
                HeaderItem("Created by"),
                HeaderItem("Last modified"),
                HeaderItem("Size"),
                HeaderItem("Sha256")
            )
            val itemList = nodeList.map { NodeListViewItem.from(it) }.sorted()
            val rowList = itemList.map {
                RowItem(listOf(it.name, it.createdBy, it.lastModified, it.size, it.sha256))
            }
            viewModelService.render(currentPath, headerList, rowList)
        }
    }

    @Permission(type = ResourceType.REPO, action = PermissionAction.DELETE)
    override fun deletePackage(userId: String, artifactInfo: ArtifactInfo) {
        val context = ArtifactRemoveContext()
        ArtifactContextHolder.getRepository().remove(context)
    }

    @Permission(type = ResourceType.REPO, action = PermissionAction.DELETE)
    override fun deleteVersion(userId: String, artifactInfo: ArtifactInfo) {
        val context = ArtifactRemoveContext()
        ArtifactContextHolder.getRepository().remove(context)
    }

    @Permission(type = ResourceType.REPO, action = PermissionAction.DELETE)
    override fun deleteDependency(mavenArtifactInfo: MavenArtifactInfo) {
        val context = ArtifactRemoveContext()
        ArtifactContextHolder.getRepository().remove(context)
    }

    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    override fun getVersionDetail(userId: String, artifactInfo: ArtifactInfo): MavenArtifactVersionData {
        val context = ArtifactQueryContext()
        return ArtifactContextHolder.getRepository().query(context) as MavenArtifactVersionData
    }

    override fun verifyDeploy(mavenArtifactInfo: MavenArtifactInfo, request: MavenWebDeployRequest) {
        val projectId = mavenArtifactInfo.projectId
        val repoName = mavenArtifactInfo.repoName
        val repo = repositoryService.getRepoDetail(projectId, repoName)
            ?: throw ErrorCodeException(ArtifactMessageCode.REPOSITORY_NOT_FOUND, repoName)
        val (model, pom, file) = artifactInfo(request)
        try {
            // 以mavenVersion是否为空来决定snapshot or release
            var mavenVersion = try {
                request.uuid.substringAfterLast("/").resolverName(model.artifactId, model.version)
            } catch (e: MavenArtifactFormatException) {
                logger.warn("Failed to parse maven version for [$projectId, $repoName, ${request.uuid}]")
                null
            }
            val classifier = if (request.classifier.isNullOrBlank()) mavenVersion?.classifier else request.classifier
            // 此处至空，表示是release版本
            mavenVersion = null
            // 在接下来的if 中，如果是snapshot版本，会对mavenVersion重新赋值
            if (model.version.isSnapshotUri()) {
                mavenVersion = getSnapshotVersion(mavenArtifactInfo, model, classifier)
            }
            logger.info("[ $projectId, $repoName, ${request.uuid} ] maven version is $mavenVersion")
            if (model.packaging != SOURCE_POM) {
                uploadArtifact(
                    repo,
                    createArtifact(mavenArtifactInfo, model, mavenVersion, classifier = classifier),
                    Files.newInputStream(file)
                )
            }
            uploadArtifact(repo, createPom(mavenArtifactInfo, model, mavenVersion), pom.inputStream())
            if (mavenVersion != null) {
                // version/maven-metadata.xml
                uploadArtifact(repo, createSnapshotMetadata(mavenArtifactInfo, model), "".byteInputStream())
            }
            // maven-metadata.xml
            updateMetadata(repo, model)
            nodeService.deleteNode(
                NodeDeleteRequest(
                    projectId = mavenArtifactInfo.projectId,
                    repoName = mavenArtifactInfo.repoName,
                    fullPath = request.uuid,
                    operator = "maven-web-deploy"
                )
            )
        } finally {
            Files.deleteIfExists(file)
        }
    }

    override fun buildVersionDeleteArtifactInfo(
        projectId: String,
        repoName: String,
        packageKey: String,
        version: String
    ) = MavenDeleteArtifactInfo(
        projectId = projectId,
        repoName = repoName,
        artifactUri = MavenUtil.extractPath(packageKey) + "/$MAVEN_METADATA_FILE_NAME",
        packageName = packageKey,
        version = version
    )

    /**
     * 根据请求信息获取 artifact 的详细信息。
     * 此函数主要用于处理 Maven 构建过程中对 artifact 信息的请求。
     * 它会根据请求的类型，从临时存储中读取相应的 POM 文件或压缩包中的 POM 信息，
     * 并与请求中的信息进行匹配，以确保所操作的 artifact 是正确的。
     *
     * @param request 包含请求的详细信息，如 UUID、组 ID、artifact ID、版本和类型。
     * @return 返回一个 Triple 对象，包含 Model（artifact 的元数据）、字节数组（POM 文件内容）和文件路径（临时存储路径）。
     * @throws NodeNotFoundException 如果根据 UUID 找不到对应的临时文件，则抛出此异常。
     */
    private fun artifactInfo(request: MavenWebDeployRequest): Triple<Model, ByteArray, Path> {
        // 获取临时文件路径并根据请求的 UUID 解析出具体的文件路径。
        val path = storageService.getTempPath().resolve(request.uuid)
        // 检查文件是否存在，如果不存在则抛出异常。
        if (Files.notExists(path)) {
            throw NodeNotFoundException(request.uuid)
        }
        // 如果请求的类型是 SOURCE_POM，则直接读取文件内容并解析为 Model 对象。
        if (request.type == SOURCE_POM) {
            val bytes = Files.readAllBytes(path)
            return Triple(readModel(bytes.inputStream()), bytes, path)
        }
        // 如果提取的 POM 文件信息与请求不匹配，则创建一个新的 Model 对象。
        val model = Model().apply {
            this.groupId = request.groupId
            this.artifactId = request.artifactId
            this.version = request.version
            this.packaging = request.type
        }

        // 将新的 Model 对象写入到字节数组输出流中。
        val os = ByteArrayOutputStream()
        MavenXpp3Writer().write(os, model)
        // 返回包含新 Model、字节数组和路径的 Triple 对象。
        return Triple(model, os.toByteArray(), path)
    }


    private fun getSnapshotVersion(
        mavenArtifactInfo: MavenArtifactInfo,
        model: Model,
        classifier: String?
    ): MavenVersion {
        val record = mavenMetadataService.findAndModify(
            MavenMetadataSearchPojo(
                projectId = mavenArtifactInfo.projectId,
                repoName = mavenArtifactInfo.repoName,
                groupId = model.groupId,
                artifactId = model.artifactId,
                version = model.version,
                classifier = classifier,
                extension = model.packaging
            )
        )
        return MavenVersion(
            artifactId = model.artifactId,
            version = model.version,
            timestamp = record.timestamp,
            buildNo = record.buildNo,
            classifier = classifier,
            packaging = model.packaging
        )
    }

    /**
     * 更新maven-metadata.xml
     */
    private fun updateMetadata(repo: RepositoryDetail, model: Model) {
        // 查询maven-metadata.xml是否存在
        val metadataPath = model.toMetadataUri()
        val metadataNode = nodeService.getNodeDetail(repo.projectId, repo.name, metadataPath).data
        val mavenMetadata = if (metadataNode != null) {
            val metadata = storageManager.loadArtifactInputStream(metadataNode, null)?.use { inputStream ->
                MetadataXpp3Reader().read(inputStream)
            }
            // TODO 是否存在maven-metadata.xml 中versioning节点不存在的情况
            metadata?.apply {
                if (!versioning.versions.contains(model.version)) {
                    versioning.versions.add(model.version)
                }
            }?.reRender()
        } else {
            Metadata().initByModel(model)
        }

        // 上传maven-metadata.xml
        val metadataArtifact = MavenArtifactInfo(
            projectId = repo.projectId,
            repoName = repo.name,
            artifactUri = model.toMetadataUri(),
        )
        ByteArrayOutputStream().use { metadataStream ->
            MetadataXpp3Writer().write(metadataStream, mavenMetadata)
            uploadArtifact(repo, metadataArtifact, metadataStream.toByteArray().inputStream())
        }
    }

    private fun createArtifact(
        mavenArtifactInfo: MavenArtifactInfo,
        model: Model,
        mavenVersion: MavenVersion? = null,
        classifier: String? = null
    ): MavenArtifactInfo {
        return MavenArtifactInfo(
            projectId = mavenArtifactInfo.projectId,
            repoName = mavenArtifactInfo.repoName,
            artifactUri = if (mavenVersion == null) {
                model.toArtifactUri(classifier)
            } else {
                model.toSnapshotArtifactUri(mavenVersion)
            }
        ).apply {
            this.groupId = model.groupId
            this.artifactId = model.artifactId
            this.versionId = model.version
            this.jarName = model.toArtifact(classifier)
        }
    }

    private fun createSnapshotMetadata(
        mavenArtifactInfo: MavenArtifactInfo,
        model: Model,
    ): MavenArtifactInfo {
        return MavenArtifactInfo(
            projectId = mavenArtifactInfo.projectId,
            repoName = mavenArtifactInfo.repoName,
            artifactUri = model.toSnapshotMetadataUri(),
        )
    }

    private fun createPom(
        mavenArtifactInfo: MavenArtifactInfo,
        model: Model,
        mavenVersion: MavenVersion? = null
    ): MavenArtifactInfo {
        return MavenArtifactInfo(
            projectId = mavenArtifactInfo.projectId,
            repoName = mavenArtifactInfo.repoName,
            artifactUri = if (mavenVersion == null) {
                model.toPomUri()
            } else {
                model.toSnapshotPomUri(mavenVersion)
            }
        ).apply {
            this.groupId = model.groupId
            this.artifactId = model.artifactId
            this.versionId = model.version
            this.jarName = model.toPom()
        }
    }

    private fun uploadArtifact(repo: RepositoryDetail, artifact: MavenArtifactInfo, inputStream: InputStream) {
        inputStream.use {
            ArtifactFileFactory.build(it).apply {
                ArtifactUploadContext(
                    repo = repo,
                    artifactFile = this,
                    artifactInfo = artifact,
                ).apply { repository.upload(this) }
                this.delete()
            }
        }
    }

    override fun extractGavFromPom(file: MultipartFile): MavenWebDeployResponse {
        val filename = file.getFilename()
        val model = readModel(file.inputStream)
        if (filename != "${model.artifactId}-${model.version}.pom") {
            throw JarFormatException("invalid pom file")
        }
        return model.toGav("")
    }

    private fun MultipartFile.getFilename(): String {
        val filename = originalFilename?.substringAfterLast("/")
        if (filename.isNullOrBlank()) {
            throw JarFormatException("invalid filename")
        }
        return filename
    }

    private fun Model.toGav(uuid: String) = MavenWebDeployResponse(
        uuid = uuid,
        groupId = this.groupId.orEmpty(),
        artifactId = this.artifactId.orEmpty(),
        version = this.version.orEmpty(),
        classifier = null,
        type = this.packaging.orEmpty()
    )


    override fun extractGavFromJar(file: MultipartFile): MavenWebDeployResponse {
        val filename = file.getFilename()
        val bytes = file.bytes
        val model = if (!filename.endsWith(".pom")) JarUtils.parseModel(bytes.inputStream()) else {
            readModel(bytes.inputStream()).apply {
                if (this.packaging != SOURCE_POM) {
                    throw JarFormatException("The packaging of the pom file is not pom")
                }
            }
        }
        val path = storageService.getTempPath().resolve(filename)
        if (Files.notExists(path.parent)) {
            Files.createDirectories(path.parent)
        }
        return Files.write(path, bytes).let { model.toGav(filename) }
    }


    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MavenServiceImpl::class.java)
    }
}

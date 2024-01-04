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
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.constant.PARAM_DOWNLOAD
import com.tencent.bkrepo.common.artifact.exception.NodeNotFoundException
import com.tencent.bkrepo.common.artifact.manager.StorageManager
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.core.ArtifactService
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.view.ViewModelService
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.maven.artifact.MavenArtifactInfo
import com.tencent.bkrepo.maven.artifact.MavenDeleteArtifactInfo
import com.tencent.bkrepo.maven.exception.JarFormatException
import com.tencent.bkrepo.maven.exception.MavenArtifactFormatException
import com.tencent.bkrepo.maven.exception.MavenBadRequestException
import com.tencent.bkrepo.maven.pojo.MavenMetadataSearchPojo
import com.tencent.bkrepo.maven.pojo.MavenVersion
import com.tencent.bkrepo.maven.pojo.request.MavenWebDeployRequest
import com.tencent.bkrepo.maven.pojo.response.MavenWebDeployResponse
import com.tencent.bkrepo.maven.service.MavenMetadataService
import com.tencent.bkrepo.maven.service.MavenService
import com.tencent.bkrepo.maven.util.JarUtils
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
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.pojo.list.HeaderItem
import com.tencent.bkrepo.repository.pojo.list.RowItem
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.NodeListViewItem
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import org.apache.commons.io.FileUtils
import org.apache.maven.artifact.repository.metadata.Metadata
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Writer
import org.apache.maven.model.Model
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.*
import java.util.regex.PatternSyntaxException

@Service
class MavenServiceImpl(
    private val nodeClient: NodeClient,
    private val viewModelService: ViewModelService,
    private val repositoryClient: RepositoryClient,
    private val storageManager: StorageManager,
    private val mavenMetadataService: MavenMetadataService
) : ArtifactService(), MavenService {

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
            throw MavenBadRequestException(e.message)
        }
    }

    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    override fun dependency(mavenArtifactInfo: MavenArtifactInfo) {
        // 为了兼容jfrog，当查询到目录时，会展示当前目录下所有子项，而不是直接报错
        with(mavenArtifactInfo) {
            val node = nodeClient.getNodeDetail(projectId, repoName, getArtifactFullPath()).data
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
            val nodeList = nodeClient.listNode(
                projectId = projectId,
                repoName = repoName,
                path = getArtifactFullPath(),
                includeFolder = true,
                deep = false
            ).data
            val currentPath = viewModelService.computeCurrentPath(node)
            val headerList = listOf(
                HeaderItem("Name"),
                HeaderItem("Created by"),
                HeaderItem("Last modified"),
                HeaderItem("Size"),
                HeaderItem("Sha256")
            )
            val itemList = nodeList?.map { NodeListViewItem.from(it) }?.sorted()
            val rowList = itemList?.map {
                RowItem(listOf(it.name, it.createdBy, it.lastModified, it.size, it.sha256))
            } ?: listOf()
            viewModelService.render(currentPath, headerList, rowList)
        }
    }

    @Permission(type = ResourceType.REPO, action = PermissionAction.DELETE)
    override fun delete(mavenArtifactInfo: MavenDeleteArtifactInfo, packageKey: String, version: String?) {
        val context = ArtifactRemoveContext()
        ArtifactContextHolder.getRepository().remove(context)
    }

    @Permission(type = ResourceType.REPO, action = PermissionAction.DELETE)
    override fun deleteDependency(mavenArtifactInfo: MavenArtifactInfo) {
        val context = ArtifactRemoveContext()
        ArtifactContextHolder.getRepository().remove(context)
    }

    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    override fun artifactDetail(mavenArtifactInfo: MavenArtifactInfo, packageKey: String, version: String?): Any? {
        val context = ArtifactQueryContext()
        return ArtifactContextHolder.getRepository().query(context)
    }

    @Permission(type = ResourceType.REPO, action = PermissionAction.WRITE)
    override fun fileDeploy(mavenArtifactInfo: MavenArtifactInfo, file: ArtifactFile): MavenWebDeployResponse {
        val context = ArtifactUploadContext(file)
        val webNode = buildMavenWebDeployNode(mavenArtifactInfo, file)
        storageManager.storeArtifactFile(webNode, file, context.storageCredentials)

        with(mavenArtifactInfo) {
            val node = nodeClient.getNodeDetail(projectId, repoName, getArtifactFullPath()).data!!
            storageManager.loadArtifactInputStream(node, context.storageCredentials).use {
                val tempFile = File.createTempFile("maven-web", "deploy")
                FileUtils.copyInputStreamToFile(it, tempFile)
                try {
                    // 依靠文件名后缀判断是否为pom
                    val model = if (getArtifactFullPath().endsWith(".pom")) {
                        JarUtils.readModel(tempFile).apply {
                            if (this.packaging != "pom") {
                                throw JarFormatException("The packaging of the pom file is not pom")
                            }
                        }
                    } else {
                        JarUtils.parseModelInJar(tempFile)
                    }
                    // 尝试解析classifier
                    val classifier = try {
                        getArtifactFullPath()
                            .substringAfterLast("/").resolverName(model.artifactId, model.version).classifier
                    } catch (e: MavenArtifactFormatException) {
                        logger.warn("Failed to parse classifier for [$projectId, $repoName, ${getArtifactFullPath()}]")
                        null
                    }
                    return MavenWebDeployResponse(
                        uuid = getArtifactFullPath(),
                        groupId = model.groupId,
                        artifactId = model.artifactId,
                        version = model.version,
                        classifier = classifier,
                        type = model.packaging
                    )
                } catch (e: JarFormatException) {
                    logger.error("Fail to parse the pom file in the jar file", e)
                    nodeClient.deleteNode(
                        NodeDeleteRequest(
                            projectId = projectId,
                            repoName = repoName,
                            fullPath = getArtifactFullPath(),
                            operator = "maven-web-deploy"
                        )
                    )
                    throw e
                } finally {
                    tempFile.apply { if (this.exists()) delete() }
                }
            }
        }
    }
    @Permission(type = ResourceType.REPO, action = PermissionAction.DELETE)
    override fun fileDeployCancel(mavenArtifactInfo: MavenArtifactInfo): Boolean {
        with(mavenArtifactInfo) {
            val node = nodeClient.getNodeDetail(projectId, repoName, getArtifactFullPath()).data
            node?.let {
                nodeClient.deleteNode(
                    NodeDeleteRequest(
                        projectId = projectId,
                        repoName = repoName,
                        fullPath = getArtifactFullPath(),
                        operator = SecurityUtils.getUserId()
                    )
                )
            }
            return true
        }
    }

    private fun buildMavenWebDeployNode(mavenArtifactInfo: MavenArtifactInfo, file: ArtifactFile): NodeCreateRequest {
        return NodeCreateRequest(
            projectId = mavenArtifactInfo.projectId,
            repoName = mavenArtifactInfo.repoName,
            fullPath = mavenArtifactInfo.getArtifactFullPath(),
            folder = false,
            overwrite = true,
            expires = 0L,
            md5 = file.getFileMd5(),
            sha256 = file.getFileSha256(),
            size = file.getSize(),
            operator = SecurityUtils.getUserId()
        )
    }

    override fun verifyDeploy(mavenArtifactInfo: MavenArtifactInfo, request: MavenWebDeployRequest) {
        with(mavenArtifactInfo) {
            val node = nodeClient.getNodeDetail(projectId, repoName, request.uuid).data
                ?: throw NodeNotFoundException(request.uuid)
            storageManager.loadArtifactInputStream(node, null)?.use {
                val repo = repositoryClient.getRepoDetail(projectId, repoName).data
                    ?: throw ErrorCodeException(ArtifactMessageCode.REPOSITORY_NOT_FOUND, repoName)
                val jarFile = File("$system_temp_dir/${UUID.randomUUID()}", request.uuid.trim('/'))
                FileUtils.copyInputStreamToFile(it, jarFile)
                val (pomFile, model) = if (node.name.endsWith(".pom")) {
                    Pair(jarFile, JarUtils.readModel(jarFile).apply { JarUtils.processModel(this, request) })
                } else {
                    JarUtils.parsePomInJar(jarFile).apply {
                        Pair(this.first, JarUtils.processModel(this.second, request))
                    }
                }
                // 以mavenVersion是否为空来决定snapshot or release
                var mavenVersion: MavenVersion? = try {
                    request.uuid
                        .substringAfterLast("/").resolverName(model.artifactId, model.version)
                } catch (e: MavenArtifactFormatException) {
                    logger.warn("Failed to parse maven version for [$projectId, $repoName, ${request.uuid}]")
                    null
                }
                val classifier = if (request.classifier.isNullOrBlank()) {
                    mavenVersion?.classifier
                } else {
                    request.classifier
                }
                // 此处至空，表示是release版本
                mavenVersion = null
                // 在接下来的if 中，如果是snapshot版本，会对mavenVersion重新赋值
                if (model.version.isSnapshotUri()) {
                    mavenVersion = getSnapshotVersion(mavenArtifactInfo, model, classifier)
                }
                logger.info("[ $projectId, $repoName, ${request.uuid} ] maven version is $mavenVersion")
                if (model.packaging != "pom") {
                    createArtifact(mavenArtifactInfo, model, mavenVersion, classifier = classifier).let { jarArtifact ->
                        uploadArtifact(repo, jarArtifact, FileInputStream(jarFile))
                        jarFile.apply { if (this.exists()) delete() }
                    }
                }
                createPom(mavenArtifactInfo, model, mavenVersion).let { pomArtifact ->
                    uploadArtifact(repo, pomArtifact, FileInputStream(pomFile))
                    pomFile.apply { if (this.exists()) delete() }
                }
                if (mavenVersion != null) {
                    // version/maven-metadata.xml
                    uploadArtifact(repo, createSnapshotMetadata(mavenArtifactInfo, model), "".byteInputStream())
                }
                // maven-metadata.xml
                updateMetadata(repo, model)
            }
        }
        nodeClient.deleteNode(
            NodeDeleteRequest(
                projectId = mavenArtifactInfo.projectId,
                repoName = mavenArtifactInfo.repoName,
                fullPath = request.uuid,
                operator = "maven-web-deploy"
            )
        )
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
        val metadataNode = nodeClient.getNodeDetail(repo.projectId, repo.name, metadataPath).data
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

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MavenServiceImpl::class.java)
        private val system_temp_dir = System.getProperty("java.io.tmpdir")
    }
}

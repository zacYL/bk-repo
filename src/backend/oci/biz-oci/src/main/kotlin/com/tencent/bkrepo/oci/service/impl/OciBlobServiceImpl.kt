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

package com.tencent.bkrepo.oci.service.impl

import com.fasterxml.jackson.databind.JsonNode
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.common.api.constant.CharPool
import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.util.DecompressUtils
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.constant.ARTIFACT_INFO_KEY
import com.tencent.bkrepo.common.artifact.hash.sha256
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.security.manager.PermissionManager
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.oci.constant.BLOB_PATH_VERSION_KEY
import com.tencent.bkrepo.oci.constant.BLOB_PATH_VERSION_VALUE
import com.tencent.bkrepo.oci.constant.IMAGE_CONFIG_MEDIA_TYPE
import com.tencent.bkrepo.oci.constant.LAYER_TAR_MEDIA_TYPE
import com.tencent.bkrepo.oci.constant.OCI_IMAGE_MANIFEST_MEDIA_TYPE
import com.tencent.bkrepo.oci.constant.OciMessageCode
import com.tencent.bkrepo.oci.constant.REPO_TYPE
import com.tencent.bkrepo.oci.exception.OciBadRequestException
import com.tencent.bkrepo.oci.exception.OciImageUploadException
import com.tencent.bkrepo.oci.model.ConfigDescriptor
import com.tencent.bkrepo.oci.model.Descriptor
import com.tencent.bkrepo.oci.model.DescriptorPath
import com.tencent.bkrepo.oci.model.Index
import com.tencent.bkrepo.oci.model.ManifestSchema2
import com.tencent.bkrepo.oci.model.OldManifest
import com.tencent.bkrepo.oci.model.LayerDescriptor
import com.tencent.bkrepo.oci.pojo.artifact.OciArtifactInfo
import com.tencent.bkrepo.oci.pojo.artifact.OciBlobArtifactInfo
import com.tencent.bkrepo.oci.pojo.artifact.OciManifestArtifactInfo
import com.tencent.bkrepo.oci.pojo.digest.OciDigest
import com.tencent.bkrepo.oci.pojo.response.ResponseProperty
import com.tencent.bkrepo.oci.service.OciBlobService
import com.tencent.bkrepo.oci.service.OciOperationService
import com.tencent.bkrepo.oci.util.ObjectBuildUtils
import com.tencent.bkrepo.oci.util.OciLocationUtils
import com.tencent.bkrepo.oci.util.OciResponseUtils
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.pojo.metadata.MetadataModel
import org.apache.commons.compress.archivers.ArchiveException
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.slf4j.LoggerFactory
import org.springframework.data.util.CastUtils
import org.springframework.stereotype.Service
import org.springframework.util.ObjectUtils
import org.springframework.util.StreamUtils
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

@Service
class OciBlobServiceImpl(
    private val storage: StorageService,
    private val repoClient: RepositoryClient,
    private val nodeClient: NodeClient,
    private val ociOperationService: OciOperationService,
    private val permissionManager: PermissionManager
) : OciBlobService {

    override fun startUploadBlob(artifactInfo: OciBlobArtifactInfo, artifactFile: ArtifactFile) {
        with(artifactInfo) {
            logger.info(
                "Handling bolb upload request ${artifactInfo.digest}|${artifactInfo.uuid}|" +
                        "${artifactInfo.mount}|${artifactInfo.from} in ${getRepoIdentify()}."
            )
            if (digest.isNullOrBlank()) {
                logger.info("Will use post then put to upload blob...")
                // docker manifest mount upload blob
                obtainSessionIdForUpload(artifactInfo)
            } else {
                logger.info("Will use single post to upload blob...")
                singlePostUpload(artifactFile)
            }
        }
    }

    /**
     * 使用单个post请求直接上传文件
     */
    private fun singlePostUpload(artifactFile: ArtifactFile) {
        val context = ArtifactUploadContext(artifactFile)
        ArtifactContextHolder.getRepository().upload(context)
    }

    /**
     * 获取上传文件uuid
     */
    private fun obtainSessionIdForUpload(artifactInfo: OciBlobArtifactInfo) {
        with(artifactInfo) {
            if (mount.isNullOrBlank()) {
                logger.info("Will obtain uuid for uploading blobs in repo ${artifactInfo.getRepoIdentify()}.")
                val uuidCreated = startAppend(this)
                val domain = ociOperationService.getReturnDomain(HttpContextHolder.getRequest())
                val responseProperty = ResponseProperty(
                    uuid = uuidCreated,
                    location = OciLocationUtils.blobUUIDLocation(uuidCreated, artifactInfo),
                    status = HttpStatus.ACCEPTED,
                    contentLength = 0
                )
                OciResponseUtils.buildUploadResponse(
                    domain,
                    responseProperty,
                    HttpContextHolder.getResponse()
                )
            } else {
                mountBlob(artifactInfo)
            }
        }
    }

    private fun mountBlob(artifactInfo: OciBlobArtifactInfo) {
        with(artifactInfo) {
            val domain = ociOperationService.getReturnDomain(HttpContextHolder.getRequest())
            val ociDigest = OciDigest(mount)
            val (mountProjectId, mountRepoName) = splitRepoInfo(from) ?: Pair(projectId, repoName)
            if (mountProjectId != projectId && mountRepoName != repoName) {
                try {
                    permissionManager.checkRepoPermission(
                        action = PermissionAction.READ,
                        projectId = mountProjectId,
                        repoName = mountRepoName
                    )
                } catch (e: ErrorCodeException) {
                    buildSessionIdLocationForUpload(this, domain)
                    return
                }
            }
            val nodeProperty = ociOperationService.getNodeByDigest(
                mountProjectId, mountRepoName, ociDigest.toString()
            ) ?: run {
                logger.warn("Could not find $ociDigest in repo $mountProjectId|$mountRepoName to mount")
                buildSessionIdLocationForUpload(this, domain)
                return
            }
            // 用于新版本 blobs 路径区分
            val metadata: MutableList<MetadataModel> = mutableListOf(
                MetadataModel(key = BLOB_PATH_VERSION_KEY, value = BLOB_PATH_VERSION_VALUE, system = true)
            )
            val nodeCreateRequest = ObjectBuildUtils.buildNodeCreateRequest(
                projectId = projectId,
                repoName = repoName,
                size = nodeProperty.size!!,
                sha256 = ociDigest.hex,
                fullPath = OciLocationUtils.buildDigestBlobsPath(packageName, ociDigest),
                md5 = nodeProperty.md5!!,
                metadata = metadata
            )
            nodeClient.createNode(nodeCreateRequest)
            val blobLocation = OciLocationUtils.blobLocation(ociDigest, this)
            val responseProperty = ResponseProperty(
                location = blobLocation,
                status = HttpStatus.CREATED
            )
            OciResponseUtils.buildUploadResponse(
                domain = domain,
                responseProperty = responseProperty,
                response = HttpContextHolder.getResponse()
            )
        }
    }

    private fun buildSessionIdLocationForUpload(artifactInfo: OciBlobArtifactInfo, domain: String) {
        val uuidCreated = startAppend(artifactInfo)
        val responseProperty = ResponseProperty(
            uuid = uuidCreated,
            location = OciLocationUtils.blobUUIDLocation(uuidCreated, artifactInfo),
            status = HttpStatus.ACCEPTED,
            contentLength = 0
        )
        OciResponseUtils.buildUploadResponse(
            domain = domain,
            responseProperty = responseProperty,
            response = HttpContextHolder.getResponse()
        )
    }

    private fun splitRepoInfo(from: String?): Pair<String, String>? {
        if (from.isNullOrEmpty()) return null
        val values = from.split(CharPool.SLASH)
        return Pair(values[0], values[1])
    }

    /**
     * start a append upload
     * @return String append Id
     */
    private fun startAppend(artifactInfo: OciArtifactInfo): String {
        with(artifactInfo) {
            // check repository
            val result = repoClient.getRepoDetail(projectId, repoName, REPO_TYPE).data ?: run {
                ArtifactContextHolder.queryRepoDetailFormExtraRepoType(projectId, repoName)
            }
            logger.debug("Start to append file in ${getRepoIdentify()}")
            return storage.createAppendId(result.storageCredentials)
        }
    }

    override fun uploadBlob(artifactInfo: OciBlobArtifactInfo, artifactFile: ArtifactFile) {
        logger.info("handing request upload blob [$artifactInfo] in repo ${artifactInfo.getRepoIdentify()}.")
        val context = ArtifactUploadContext(artifactFile)
        // 3种上传方式都在local里面做处理
        ArtifactContextHolder.getRepository().upload(context)
    }

    override fun downloadBlob(artifactInfo: OciBlobArtifactInfo) {
        with(artifactInfo) {
            logger.info(
                "Handling blob download request for blob [${getDigest()}] in repo [${artifactInfo.getRepoIdentify()}]"
            )
            val context = ArtifactDownloadContext()
            ArtifactContextHolder.getRepository().download(context)
        }
    }

    override fun deleteBlob(artifactInfo: OciBlobArtifactInfo) {
        logger.info(
            "Handling delete blob request for package [${artifactInfo.packageName}] " +
                    "with digest [${artifactInfo.digest}] in repo [${artifactInfo.getRepoIdentify()}]"
        )
        if (artifactInfo.digest.isNullOrBlank()) {
            throw OciBadRequestException(OciMessageCode.OCI_DELETE_RULES, artifactInfo.getArtifactFullPath())
        }
        val context = ArtifactRemoveContext()
        ArtifactContextHolder.getRepository().remove(context)
    }

    // 避免过多上传请求
    private val uploading = AtomicBoolean(false)

    //
    override fun uploadImage(artifactInfo: OciArtifactInfo, file: MultipartFile) {
        if (!uploading.compareAndSet(false, true)) {
            throw OciImageUploadException(OciMessageCode.OCI_IMAGE_UPLOADING)
        }
        try {
            val (index, manifestBytes) = decompressImage(file.inputStream)
            if (index == null) {
                manifestBytes.inputStream().readJsonString<List<OldManifest>>().transferOldImage().forEach { e ->
                    uploadImage(e, artifactInfo) { CastUtils.cast<DescriptorPath>(it).path!!.toFile() }
                }
            } else {
                val layer: (String) -> File = layer@{
                    return@layer storage
                        .getTempPath(null)
                        .resolve("blobs/${it.replace(":", "/")}")
                        .toFile()
                }
                index.manifests.forEach { e ->
                    layer(e.digest).inputStream().readJsonString<ManifestSchema2>().apply {
                        uploadImage(this, artifactInfo) { layer(it.digest) }
                    }
                }
            }
        } finally {
            uploading.lazySet(false)
        }
    }

    private fun decompressImage(inputStream: InputStream): Pair<Index?, ByteArray> {
        logger.info("start decompress docker image")
        val path = storage.getTempPath()
        var index: Index? = null
        var manifest: ByteArray? = null
        try {
            DecompressUtils.tryArchiverWithCompressor<Unit, Unit>(
                inputStream,
                callback = { stream, entry ->
                    if (entry is TarArchiveEntry && entry.isSymbolicLink) {
                        return@tryArchiverWithCompressor
                    }
                    if (entry.name == "manifest.json") {
                        manifest = StreamUtils.copyToByteArray(stream)
                        return@tryArchiverWithCompressor
                    }
                    if (entry.name == "index.json") {
                        index = StreamUtils.copyToByteArray(stream).inputStream().readJsonString()
                        return@tryArchiverWithCompressor
                    }
                    val layer = path.resolve(entry.name)
                    if (Files.notExists(layer)) {
                        if (Files.notExists(layer.parent)) {
                            Files.createDirectories(layer.parent)
                        }
                        Files.newOutputStream(layer).use { StreamUtils.copy(stream, it) }
                    }
                },
                handleResult = { _, _, _ -> }
            )
        } catch (e: ArchiveException) {
            logger.error("Illegal image files!", e)
            throw OciImageUploadException(OciMessageCode.OCI_IMAGE_INVALID)
        }
        if (index == null && ObjectUtils.isEmpty(manifest)) {
            throw OciImageUploadException(OciMessageCode.OCI_IMAGE_INVALID)
        }
        return index to manifest!!
    }

    /**
     * 将旧的镜像清单转换为新的ManifestSchema2格式
     * 此函数用于处理和转换旧的镜像清单数据结构到新的格式，以确保兼容性和更新
     * 它首先检查和准备必要的层，然后根据需要更新配置文件，以生成最终的新的镜像清单
     *
     * @return 转换后的ManifestSchema2列表，表示更新后的镜像清单
     */
    private fun List<OldManifest>.transferOldImage(): List<ManifestSchema2> {
        logger.info("will start transfer the manifest to manifest Schema2")
        val path = storage.getTempPath()
        return map { e ->
            // 标记是否需要重置配置
            var resetConfig = false
            // 处理和转换层，如果层不存在，则标记需要重置配置，并跳过该层
            val layers = e.layers.mapNotNull {
                val layer = path.resolve(it)
                if (Files.notExists(layer)) {
                    resetConfig = true
                    return@mapNotNull null
                }
                return@mapNotNull LayerDescriptor(
                    mediaType = LAYER_TAR_MEDIA_TYPE,
                    size = Files.size(layer),
                    digest = "sha256:${Files.newInputStream(layer).sha256()}",
                    path = layer
                )
            }

            // 根据是否需要重置配置，处理配置文件
            val config = if (!resetConfig) path.resolve(e.config).toConfigDescriptor() else {
                // 读取配置文件的JSON内容，去除因软链问题可能的重复项
                val json = path.resolve(e.config).toFile().inputStream().readJsonString<JsonNode>()
                val digest = mutableSetOf<String>()
                val iterator = json.at("/rootfs/diff_ids").iterator()
                while (iterator.hasNext()) {
                    if (!digest.add(iterator.next().asText())) {
                        iterator.remove()
                    }
                }
                // 将更新后的JSON内容转换回字符串，并保存到新的文件中
                val jsonStr = json.toJsonString()
                path
                    .resolve(jsonStr.sha256())
                    .apply { Files.newOutputStream(this).use { StreamUtils.copy(jsonStr.toByteArray(), it) } }
                    .toConfigDescriptor()
            }
            // 返回映射到新的ManifestSchema2格式的镜像清单
            return@map ManifestSchema2(2, OCI_IMAGE_MANIFEST_MEDIA_TYPE, config, layers)
        }
    }

    private fun Path.toConfigDescriptor() = ConfigDescriptor(
        IMAGE_CONFIG_MEDIA_TYPE,
        Files.size(this),
        "sha256:${Files.newInputStream(this).sha256()}",
        this
    )

    /**
     * 上传镜像到仓库
     *
     * 该函数负责将镜像文件及其关联的元数据上传到仓库中它首先解析镜像的manifest文件，
     * 然后逐层上传镜像的每一层及其配置文件最后，上传manifest本身
     *
     * @param manifest 镜像manifest
     * @param artifactInfo 镜像的相关信息，包括名称、摘要等
     */
    private fun uploadImage(
        manifest: ManifestSchema2,
        artifactInfo: OciArtifactInfo,
        layer: (Descriptor) -> File
    ) {
        // 解析manifest中的layers和config，逐个处理
        manifest.layers.plus(manifest.config).forEach { e ->
            // 获取当前层的文件流并构建ArtifactFile对象
            val file = layer(e)
                .apply { if (!exists()) return@forEach }
                .let { ArtifactFileFactory.build(layer(e).inputStream()) }
            // 创建一个唯一的UUID用于标识这次上传
            val uuid = storage.createAppendId(null)
            // 设置当前处理的blob的artifact信息到HTTP请求中
            HttpContextHolder.getRequest()
                .setAttribute(ARTIFACT_INFO_KEY, artifactInfo.toOciBlobArtifactInfo(e.digest, uuid))
            // 上传当前层的文件
            ArtifactContextHolder.getRepository().upload(ArtifactUploadContext(file))
        }
        // 设置manifest的artifact信息到HTTP请求中
        HttpContextHolder.getRequest().setAttribute(ARTIFACT_INFO_KEY, artifactInfo.toOciManifestArtifactInfo())
        // 构建manifest的ArtifactFile对象
        val context = ArtifactUploadContext(ArtifactFileFactory.build(manifest.toJsonString().byteInputStream()))
        // 上传manifest
        ArtifactContextHolder.getRepository().upload(context)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(OciBlobServiceImpl::class.java)

        private fun OciArtifactInfo.toOciBlobArtifactInfo(digest: String, uuid: String) = OciBlobArtifactInfo(
            projectId,
            repoName,
            packageName,
            version,
            digest,
            uuid,
            "",
            ""
        )

        private fun OciArtifactInfo.toOciManifestArtifactInfo() = OciManifestArtifactInfo(
            projectId,
            repoName,
            packageName,
            version,
            version,
            isValidDigest = true,
            isFat = false
        )
    }
}

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

import com.tencent.bk.audit.context.ActionAuditContext
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.common.api.constant.CharPool
import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.util.DecompressUtils
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.constant.ARTIFACT_INFO_KEY
import com.tencent.bkrepo.common.artifact.exception.RepoNotFoundException
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.metadata.permission.PermissionManager
import com.tencent.bkrepo.common.metadata.service.node.NodeService
import com.tencent.bkrepo.common.metadata.service.repo.RepositoryService
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.oci.constant.BLOB_PATH_VERSION_KEY
import com.tencent.bkrepo.oci.constant.BLOB_PATH_VERSION_VALUE
import com.tencent.bkrepo.oci.constant.OciMessageCode
import com.tencent.bkrepo.oci.constant.REPO_TYPE
import com.tencent.bkrepo.oci.exception.OciBadRequestException
import com.tencent.bkrepo.oci.exception.OciImageUploadException
import com.tencent.bkrepo.oci.model.Index
import com.tencent.bkrepo.oci.model.Manifest
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
import com.tencent.bkrepo.repository.pojo.metadata.MetadataModel
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.StreamUtils
import org.springframework.web.multipart.MultipartFile

@Service
class OciBlobServiceImpl(
    private val storage: StorageService,
    private val repositoryService: RepositoryService,
    private val nodeService: NodeService,
    private val ociOperationService: OciOperationService,
    private val permissionManager: PermissionManager,
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
            // 当mount仓库和当前仓库不在一个存储实例时，直接上传
            val mountRepo = repositoryService.getRepoDetail(mountProjectId, mountRepoName)
                ?: throw RepoNotFoundException("$mountProjectId|$mountRepoName")
            val currentRepo = repositoryService.getRepoDetail(projectId, repoName)
                ?: throw RepoNotFoundException("$projectId|$repoName")
            if (mountRepo.storageCredentials?.key != currentRepo.storageCredentials?.key) {
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
            ActionAuditContext.current().setInstance(nodeCreateRequest)
            nodeService.createNode(nodeCreateRequest)
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
            val result = repositoryService.getRepoDetail(projectId, repoName, REPO_TYPE) ?: run {
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
        if (artifactInfo.digest.isNullOrBlank())
            throw OciBadRequestException(OciMessageCode.OCI_DELETE_RULES, artifactInfo.getArtifactFullPath())
        val context = ArtifactRemoveContext()
        ArtifactContextHolder.getRepository().remove(context)
    }

    //避免过多上传请求
    private val uploading = AtomicBoolean(false)

    override fun uploadImage(artifactInfo: OciArtifactInfo, file: MultipartFile) {
        if (!uploading.compareAndSet(false, true)) {
            throw OciImageUploadException(OciMessageCode.OCI_IMAGE_UPLOADING)
        }
        try {
            val (index, blobs) = decompressImage(file.inputStream)
            index.manifests.forEach { uploadImage(it.digest, artifactInfo, blobs) }
        } finally {
            uploading.lazySet(false)
        }
    }


    /**
     * 解压缩镜像文件
     *
     * 该函数从输入流中读取数据，解压缩并解析镜像文件，同时提取索引信息和镜像数据块
     * 使用了DecompressUtils工具类来处理解压缩逻辑，其中涉及对"index.json"的特殊处理，
     * 以及对其他数据块的常规处理，并通过StreamUtils将流转换为字节数组
     *
     * @param inputStream 输入流，用于读取镜像数据
     * @return 返回一个Pair对象，包含索引信息和镜像数据块的映射
     * @throws OciImageUploadException 如果索引为空或数据块为空，则抛出异常，表示镜像无效
     */
    private fun decompressImage(inputStream: InputStream): Pair<Index, MutableMap<String, ByteArray>> {
        // 初始化索引变量为null，后续将存储index.json的内容
        var index: ByteArray? = null

        // 使用DecompressUtils尝试解压缩输入流，返回一个可变映射和字节数组
        // 该函数会尝试使用不同的解压缩器来解压输入流，直到成功为止
        val blobs = try {
            DecompressUtils.tryArchiverWithCompressor<MutableMap<String, ByteArray>, ByteArray>(
                inputStream,
                { mutableMapOf() },
                callback = { stream, entry ->
                    // 如果当前条目是index.json，将其内容复制到index变量中，并停止进一步处理
                    if (entry.name.endsWith("index.json")) {
                        index = StreamUtils.copyToByteArray(stream)
                        return@tryArchiverWithCompressor null
                    }
                    // 对于其他条目，将其内容复制为字节数组并返回
                    return@tryArchiverWithCompressor StreamUtils.copyToByteArray(stream)
                },
                handleResult = { r, e, entry ->
                    // 如果处理过程中发生异常，将异常信息存储在结果映射中
                    if (e != null) {
                        r!![entry.name] = e
                    }
                    // 返回更新后的结果映射
                    return@tryArchiverWithCompressor r
                }
            )
        } catch (e: IOException) {
            logger.error("Illegal image files!", e)
            throw OciImageUploadException(OciMessageCode.OCI_IMAGE_INVALID)
        }

        // 如果索引为空或blobs为空，抛出异常，表示镜像无效
        if (index?.isEmpty() == true || blobs.isNullOrEmpty()) {
            throw OciImageUploadException(OciMessageCode.OCI_IMAGE_INVALID)
        }

        // 解析index.json的内容，并与blobs一起作为Pair对象返回
        return Pair(index!!.inputStream().readJsonString(), blobs)
    }


    /**
     * 上传镜像到仓库
     *
     * 该函数负责将镜像文件及其关联的元数据上传到仓库中它首先解析镜像的manifest文件，
     * 然后逐层上传镜像的每一层及其配置文件最后，上传manifest本身
     *
     * @param manifestDigest 镜像manifest的摘要值，用于唯一标识镜像
     * @param artifactInfo 镜像的相关信息，包括名称、摘要等
     * @param blobs 包含所有镜像层数据的字典，键为层的摘要值，值为层的数据
     */
    private fun uploadImage(manifestDigest: String, artifactInfo: OciArtifactInfo, blobs: Map<String, ByteArray>) {
        // 获取并解析manifest层
        val manifest = getLayer(manifestDigest, blobs)
        // 解析manifest中的layers和config，逐个处理
        manifest.inputStream().readJsonString<Manifest>().let { it.layers.plus(it.config) }.forEach { e ->
            // 创建一个唯一的UUID用于标识这次上传
            val uuid = storage.createAppendId(null)
            // 设置当前处理的blob的artifact信息到HTTP请求中
            HttpContextHolder.getRequest().setAttribute(
                ARTIFACT_INFO_KEY, artifactInfo.toOciBlobArtifactInfo(e.digest, uuid)
            )
            // 获取当前层的文件流并构建ArtifactFile对象
            val file = ArtifactFileFactory.build(getLayer(e.digest, blobs).inputStream())
            // 上传当前层的文件
            ArtifactContextHolder.getRepository().upload(ArtifactUploadContext(file))
        }
        // 设置manifest的artifact信息到HTTP请求中
        HttpContextHolder.getRequest().setAttribute(ARTIFACT_INFO_KEY, artifactInfo.toOciManifestArtifactInfo())
        // 构建manifest的ArtifactFile对象
        val context = ArtifactUploadContext(ArtifactFileFactory.build(manifest.inputStream()))
        // 上传manifest
        ArtifactContextHolder.getRepository().upload(context)
    }

    private fun getLayer(digest: String, blobs: Map<String, ByteArray>): ByteArray {
        val sha256 = "blobs/${digest.replace(":", "/")}"
        return blobs[sha256] ?: run {
            logger.warn("The content of $sha256 is null")
            throw OciImageUploadException(OciMessageCode.OCI_IMAGE_INVALID)
        }
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

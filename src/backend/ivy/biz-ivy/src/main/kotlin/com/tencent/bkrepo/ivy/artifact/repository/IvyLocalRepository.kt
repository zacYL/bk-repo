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

package com.tencent.bkrepo.ivy.artifact.repository

import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.constant.DISPLAY_REPO_TYPE_KEY
import com.tencent.bkrepo.common.artifact.pojo.RepositoryIdentify
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.local.LocalRepository
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactChannel
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.artifact.stream.artifactStream
import com.tencent.bkrepo.common.metadata.service.metadata.MetadataService
import com.tencent.bkrepo.common.metadata.util.PackageKeys
import com.tencent.bkrepo.ivy.artifact.IvyArtifactInfo
import com.tencent.bkrepo.ivy.constants.METADATA_KEY_ATTRIBUTES
import com.tencent.bkrepo.ivy.constants.METADATA_KEY_All_ARTIFACT_FULL_PATH
import com.tencent.bkrepo.ivy.constants.METADATA_KEY_BRANCH
import com.tencent.bkrepo.ivy.constants.METADATA_KEY_EXTRA_ATTRIBUTES
import com.tencent.bkrepo.ivy.constants.METADATA_KEY_IVY_FULL_PATH
import com.tencent.bkrepo.ivy.constants.METADATA_KEY_MASTER_ARTIFACT
import com.tencent.bkrepo.ivy.constants.METADATA_KEY_MASTER_ARTIFACT_FULL_PATH
import com.tencent.bkrepo.ivy.constants.METADATA_KEY_NAME
import com.tencent.bkrepo.ivy.constants.METADATA_KEY_ORGANISATION
import com.tencent.bkrepo.ivy.constants.METADATA_KEY_PACKAGE_KEY
import com.tencent.bkrepo.ivy.constants.METADATA_KEY_PACKAGE_VERSION
import com.tencent.bkrepo.ivy.constants.METADATA_KEY_PUBLISH_ARTIFACT
import com.tencent.bkrepo.ivy.constants.METADATA_KEY_QUALIFIED_EXTRA_ATTRIBUTES
import com.tencent.bkrepo.ivy.constants.METADATA_KEY_REVISION
import com.tencent.bkrepo.ivy.enum.IvyMessageCode
import com.tencent.bkrepo.ivy.exception.IvyRequestForbiddenException
import com.tencent.bkrepo.ivy.pojo.ParseIvyInfo
import com.tencent.bkrepo.ivy.util.IvyUtil
import com.tencent.bkrepo.repository.constant.CoverStrategy
import com.tencent.bkrepo.repository.pojo.download.PackageDownloadRecord
import com.tencent.bkrepo.repository.pojo.metadata.MetadataModel
import com.tencent.bkrepo.repository.pojo.metadata.MetadataSaveRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionCreateRequest
import org.apache.ivy.core.module.descriptor.ModuleDescriptor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Component

@Component
class IvyLocalRepository(
    private val metadataService: MetadataService,
) : LocalRepository() {

    override fun coverStrategy(context: ArtifactUploadContext) {
        (context.artifactInfo as IvyArtifactInfo).let {
            val repoInfo = repositoryService.getRepoInfo(projectId = it.projectId, name = it.repoName)
                ?: throw IvyRequestForbiddenException(
                    IvyMessageCode.IVY_REQUEST_FORBIDDEN,
                    it.getArtifactFullPath(),
                    it.repoName
                )
            when (repoInfo.coverStrategy) {
                CoverStrategy.COVER, null -> {
                    //
                    val fullPath = it.getArtifactFullPath()
                    val node = nodeService.getNodeDetail(ArtifactInfo(it.projectId, it.repoName, fullPath))
                    if (node != null) {
                        logger.warn("node [$fullPath] already exists, delete it")
                        nodeService.deleteNode(
                            NodeDeleteRequest(
                                projectId = it.projectId,
                                repoName = it.repoName,
                                fullPath = fullPath,
                                operator = context.userId
                            )
                        )
                    }
                }

                else -> {
                    throw IvyRequestForbiddenException(
                        IvyMessageCode.IVY_ARTIFACT_COVER_FORBIDDEN,
                    )
                }
            }
        }
    }

    override fun onUpload(context: ArtifactUploadContext) {
        (context.artifactInfo as IvyArtifactInfo).let {
            if (it.isSummaryFile()) {
                // 摘要文件处理 .md5 .sha1
                handleSummaryFile(context, it)
            } else if (it.isIvyXml(context.getArtifactFile().getInputStream())) {
                // ivy文件处理
                handleIvyFileAndSaveVersion(context, it)
            }
            super.onUpload(context)
        }
    }

    private fun handleSummaryFile(context: ArtifactUploadContext, ivyArtifactInfo: IvyArtifactInfo) {
        ivyArtifactInfo.let {
            val fullPath = it.extractArtifactFilePathFromSummary()
            nodeService.getNodeDetail(ArtifactInfo(it.projectId, it.repoName, fullPath))
                ?: throw IvyRequestForbiddenException(
                    IvyMessageCode.IVY_ARTIFACT_NOT_FOUND,
                    fullPath,
                    it.getRepoIdentify()
                )
            // 客户端是先上车制品，再上传摘要文件，所以可以更新节点元数据
            metadataService.saveMetadata(
                MetadataSaveRequest(
                    projectId = context.projectId,
                    repoName = context.repoName,
                    fullPath = fullPath,
                    nodeMetadata = listOf(
                        MetadataModel(
                            key = it.getExt(),
                            value = IvyUtil.extractDigest(context.getArtifactFile().getInputStream()),
                            system = true,
                            display = true
                        )
                    )
                )
            )
        }
    }

    private fun handleIvyFileAndSaveVersion(context: ArtifactUploadContext, ivyArtifactInfo: IvyArtifactInfo) {
        ivyArtifactInfo.let {
            val artifactPattern = it.getRepoArtifactPattern(context.repositoryDetail)
            val ivyPattern = it.getRepoIvyPattern(context.repositoryDetail)
            // 解析ivy.xml获取发布的制品（可能多个），并获取发布制品中的主文件
            val parseIvyInfo =
                IvyUtil.ivyParsePublishArtifacts(
                    it.getFile(context.getArtifactFile()),
                    artifactPattern,
                    ivyPattern
                )


            // 如果主文件为空，使用ivy的fullpath
            val masterArtifactFullPath =
                parseIvyInfo.masterArtifactFullPath ?: context.artifactInfo.getArtifactFullPath()
            val metadataModels = getMetadataModel(
                context,
                ivyArtifactInfo,
                context.artifactInfo.getArtifactFullPath(),
                masterArtifactFullPath,
                parseIvyInfo
            )

            val isLegalPathIvyFile = parseIvyInfo.isLegalPathIvyFile(
                context.projectId,
                context.repoName,
                context.artifactInfo.getArtifactFullPath()
            )
            if (isLegalPathIvyFile) {
                // ivy路径合法则创建包版本
                parseIvyInfo.artifactsFullPath.forEach { fullPath ->
                    metadataService.saveMetadata(
                        MetadataSaveRequest(
                            projectId = context.projectId,
                            repoName = context.repoName,
                            fullPath = fullPath,
                            nodeMetadata = metadataModels
                        )
                    )
                }
                createIvyVersion(
                    context,
                    parseIvyInfo.model,
                    masterArtifactFullPath,
                    metadataModels
                )
            } else if (!ivyArtifactInfo.isSupportAnyPattern(context.repositoryDetail)) {
                // 是否支持任意路径模式，不支持则抛出异常
                throw IvyRequestForbiddenException(
                    IvyMessageCode.IVY_ARTIFACT_FORMAT_ERROR,
                    context.artifactInfo.getArtifactFullPath(),
                    context.artifactInfo.getRepoIdentify()
                )
            }
        }
    }

    private fun createIvyVersion(
        context: ArtifactUploadContext,
        descriptor: ModuleDescriptor,
        artifactFullPath: String,
        metaDataModels: List<MetadataModel>
    ) {
        try {
            val (packageKey, version) = getPackageKeyAndVersion(descriptor)
            packageService.createPackageVersion(
                PackageVersionCreateRequest(
                    context.projectId,
                    context.repoName,
                    packageName = descriptor.moduleRevisionId.organisation,
                    packageKey = packageKey,
                    packageType = PackageType.IVY,
                    versionName = version,
                    size = context.getArtifactFile().getSize(),
                    artifactPath = artifactFullPath,
                    overwrite = true,
                    createdBy = context.userId,
                    packageMetadata = metaDataModels
                )
            )
        } catch (ignore: DuplicateKeyException) {
            logger.warn(
                "The package info has been created for version[${descriptor.moduleRevisionId.revision}] " +
                        "and package[${
                            PackageKeys.ofIvy(
                                descriptor.moduleRevisionId.organisation,
                                descriptor.moduleRevisionId.name,
                                descriptor.moduleRevisionId?.branch
                            )
                        }] in repo ${context.artifactInfo.getRepoIdentify()}"
            )
        }
    }

    private fun getPackageKeyAndVersion(descriptor: ModuleDescriptor): Pair<String, String> {
        return Pair(
            PackageKeys.ofIvy(
                descriptor.moduleRevisionId.organisation,
                descriptor.moduleRevisionId.name,
                descriptor.moduleRevisionId?.branch
            ),
            descriptor.moduleRevisionId.revision
        )
    }

    private fun getMetadataModel(
        context: ArtifactUploadContext,
        ivyArtifactInfo: IvyArtifactInfo,
        ivyFullPath: String,
        masterArtifactFullPath: String,
        parseIvyInfo: ParseIvyInfo,
    ): MutableList<MetadataModel> {
        with(parseIvyInfo) {
            val (packageKey, version) = getPackageKeyAndVersion(model)

            val metaDataModels = mutableListOf(
                MetadataModel(
                    key = METADATA_KEY_ORGANISATION,
                    value = model.moduleRevisionId.organisation,
                    system = true,
                    display = true
                ),
                MetadataModel(
                    key = METADATA_KEY_NAME,
                    value = model.moduleRevisionId.name,
                    system = true,
                    display = true
                ),
                MetadataModel(
                    key = METADATA_KEY_REVISION,
                    value = model.moduleRevisionId.revision,
                    system = true,
                    display = true
                ),
                MetadataModel(
                    key = METADATA_KEY_ATTRIBUTES,
                    value = model.moduleRevisionId.attributes,
                    system = true,
                    display = true
                ),
                MetadataModel(
                    key = METADATA_KEY_EXTRA_ATTRIBUTES,
                    value = model.moduleRevisionId.extraAttributes,
                    system = true,
                    display = true
                ),
                MetadataModel(
                    key = METADATA_KEY_QUALIFIED_EXTRA_ATTRIBUTES,
                    value = model.moduleRevisionId.qualifiedExtraAttributes,
                    system = true,
                    display = true
                ),
                MetadataModel(
                    key = METADATA_KEY_PUBLISH_ARTIFACT,
                    value = model.allArtifacts,
                    system = true,
                    display = false
                ),
                MetadataModel(
                    key = METADATA_KEY_MASTER_ARTIFACT_FULL_PATH,
                    value = masterArtifactFullPath,
                    system = true,
                    display = true
                ),
                MetadataModel(
                    key = METADATA_KEY_IVY_FULL_PATH,
                    value = ivyFullPath,
                    system = true,
                    display = false
                ),
                MetadataModel(
                    key = METADATA_KEY_All_ARTIFACT_FULL_PATH,
                    value = artifactsFullPath,
                    system = true,
                    display = true
                ),
                MetadataModel(
                    key = METADATA_KEY_All_ARTIFACT_FULL_PATH,
                    value = artifactsFullPath,
                    system = true,
                    display = true
                ),
                MetadataModel(
                    key = METADATA_KEY_PACKAGE_KEY,
                    value = packageKey,
                    system = true,
                    display = true
                ),
                MetadataModel(
                    key = METADATA_KEY_PACKAGE_VERSION,
                    value = version,
                    system = true,
                    display = true
                ),
                MetadataModel(
                    key = DISPLAY_REPO_TYPE_KEY,
                    value = ivyArtifactInfo.getDisplayRepoType(context.repositoryDetail),
                    system = true,
                    display = true
                ),
            )
            masterArtifact?.let {
                metaDataModels.add(
                    MetadataModel(
                        key = METADATA_KEY_MASTER_ARTIFACT,
                        value = masterArtifact,
                        system = true,
                        display = false
                    ),
                )
            }
            model.moduleRevisionId?.branch?.let { branch ->
                metaDataModels.add(
                    MetadataModel(
                        key = METADATA_KEY_BRANCH,
                        value = branch,
                        system = true,
                        display = true
                    )
                )
            }
            return metaDataModels
        }
    }

    override fun onDownload(context: ArtifactDownloadContext): ArtifactResource? {
        (context.artifactInfo as IvyArtifactInfo).let {
            if (it.isSummaryFile()) {
                val fullPath = it.extractArtifactFilePathFromSummary()
                val nodeDetail = nodeService.getNodeDetail(ArtifactInfo(context.projectId, context.repoName, fullPath))
                    ?: return super.onDownload(context)
                val metadata = metadataService.listMetadata(
                    projectId = context.projectId,
                    repoName = context.repoName,
                    fullPath = fullPath
                )
                val summaryValue =
                    metadata?.get(it.getExt())?.toString()?.toByteArray() ?: return super.onDownload(context)
                val srcRepo = RepositoryIdentify(context.projectId, context.repoName)
                logger.info("获取[${fullPath}]元数据摘要文件")
                return ArtifactResource(
                    summaryValue.inputStream().artifactStream(Range.full(summaryValue.size.toLong())),
                    it.getResponseName(),
                    srcRepo,
                    nodeDetail,
                    ArtifactChannel.LOCAL,
                    context.useDisposition
                )
            } else {
                return super.onDownload(context)
            }
        }
    }


    // ivy 客户端下载统计
    override fun buildDownloadRecord(
        context: ArtifactDownloadContext,
        artifactResource: ArtifactResource
    ): PackageDownloadRecord? {
        with(context) {
            val fullPath = artifactInfo.getArtifactFullPath()
            val node = nodeService.getNodeDetail(ArtifactInfo(projectId, repoName, fullPath)) ?: return null
            val masterArtifactFullPath =
                node.nodeMetadata.firstOrNull() { it.key == METADATA_KEY_MASTER_ARTIFACT_FULL_PATH }?.value as? String
            if (fullPath == masterArtifactFullPath) {
                val packageKey = node.nodeMetadata.firstOrNull() {
                    it.key == METADATA_KEY_PACKAGE_KEY
                } ?: return null
                val version = node.nodeMetadata.firstOrNull() {
                    it.key == METADATA_KEY_PACKAGE_VERSION
                } ?: return null
                return PackageDownloadRecord(
                    projectId,
                    repoName,
                    packageKey.value.toString(),
                    version.value.toString(),
                    userId
                )
            } else {
                return null
            }
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(IvyLocalRepository::class.java)
    }
}

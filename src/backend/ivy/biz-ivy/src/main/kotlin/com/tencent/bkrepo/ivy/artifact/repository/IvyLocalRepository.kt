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

import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.local.LocalRepository
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.ivy.artifact.IvyArtifactInfo
import com.tencent.bkrepo.ivy.constants.METADATA_KEY_ATTRIBUTES
import com.tencent.bkrepo.ivy.constants.METADATA_KEY_BRANCH
import com.tencent.bkrepo.ivy.constants.METADATA_KEY_EXTRA_ATTRIBUTES
import com.tencent.bkrepo.ivy.constants.METADATA_KEY_IVY_FULL_PATH
import com.tencent.bkrepo.ivy.constants.METADATA_KEY_MASTER_ARTIFACT
import com.tencent.bkrepo.ivy.constants.METADATA_KEY_MASTER_ARTIFACT_FULL_PATH
import com.tencent.bkrepo.ivy.constants.METADATA_KEY_NAME
import com.tencent.bkrepo.ivy.constants.METADATA_KEY_ORGANISATION
import com.tencent.bkrepo.ivy.constants.METADATA_KEY_PUBLISH_ARTIFACT
import com.tencent.bkrepo.ivy.constants.METADATA_KEY_QUALIFIED_EXTRA_ATTRIBUTES
import com.tencent.bkrepo.ivy.constants.METADATA_KEY_REVISION
import com.tencent.bkrepo.ivy.enum.IvyMessageCode
import com.tencent.bkrepo.ivy.exception.IvyRequestForbiddenException
import com.tencent.bkrepo.ivy.util.IvyUtil
import com.tencent.bkrepo.repository.api.MetadataClient
import com.tencent.bkrepo.repository.pojo.metadata.MetadataModel
import com.tencent.bkrepo.repository.pojo.metadata.MetadataSaveRequest
import com.tencent.bkrepo.repository.pojo.packages.PackageType
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionCreateRequest
import org.apache.ivy.core.module.descriptor.Artifact
import org.apache.ivy.core.module.descriptor.ModuleDescriptor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Component

@Component
class IvyLocalRepository(
    private val metadataClient: MetadataClient,
) : LocalRepository() {

    override fun onUpload(context: ArtifactUploadContext) {
        (context.artifactInfo as IvyArtifactInfo).let {
            if (it.isSummaryFile()) {
                // 摘要文件处理 .md5 .sh1
                val fullPath = it.getArtifactFullPath()
                nodeClient.getNodeDetail(
                    projectId = it.projectId,
                    repoName = it.repoName,
                    fullPath = fullPath
                ).data ?: throw IvyRequestForbiddenException(
                    IvyMessageCode.IVY_ARTIFACT_NOT_FOUND, fullPath, it.getRepoIdentify()
                )
                metadataClient.saveMetadata(
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
            } else if (it.isIvyXml(context.getArtifactFile().getInputStream())) {
                // ivy文件处理
                val artifactPattern = it.getRepoArtifactPattern(context.repositoryDetail)
                val (descriptor, artifactsFullPath, masterArtifact) =
                    IvyUtil.ivyParsePublishArtifacts(
                        it.getFile(context.getArtifactFile()),
                        artifactPattern
                    )
                // 如果主文件为空，使用ivy的fullpath
                val artifactFullPath = masterArtifact.second ?: context.artifactInfo.getArtifactFullPath()
                val metadataModels = getMetadataModel(
                    descriptor,
                    context.artifactInfo.getArtifactFullPath(),
                    masterArtifact.first,
                    artifactFullPath
                )
                createIvyVersion(
                    context,
                    descriptor,
                    artifactFullPath,
                    metadataModels
                )

                artifactsFullPath.forEach { fullPath ->
                    metadataClient.saveMetadata(
                        MetadataSaveRequest(
                            projectId = context.projectId,
                            repoName = context.repoName,
                            fullPath = fullPath,
                            nodeMetadata = metadataModels
                        )
                    )
                }

            }
            super.onUpload(context)
        }
    }

    private fun createIvyVersion(
        context: ArtifactUploadContext,
        descriptor: ModuleDescriptor,
        artifactFullPath: String,
        metaDataModels: List<MetadataModel>
    ) {
        try {
            packageClient.createVersion(
                PackageVersionCreateRequest(
                    context.projectId,
                    context.repoName,
                    packageName = descriptor.moduleRevisionId.organisation,
                    packageKey = PackageKeys.ofIvy(
                        descriptor.moduleRevisionId.organisation,
                        descriptor.moduleRevisionId.name,
                        descriptor.moduleRevisionId.branch
                    ),
                    packageType = PackageType.IVY,
                    versionName = descriptor.moduleRevisionId.revision,
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
                                descriptor.moduleRevisionId.branch
                            )
                        }] in repo ${context.artifactInfo.getRepoIdentify()}"
            )
        }
    }

    private fun getMetadataModel(
        descriptor: ModuleDescriptor,
        ivyFullPath: String,
        masterArtifact: Artifact?,
        masterArtifactFullPath: String,
    ): MutableList<MetadataModel> {
        val metaDataModels = mutableListOf(
            MetadataModel(key = METADATA_KEY_ATTRIBUTES, value = descriptor.moduleRevisionId.attributes, system = true),
            MetadataModel(
                key = METADATA_KEY_ORGANISATION,
                value = descriptor.moduleRevisionId.organisation,
                system = true
            ),
            MetadataModel(key = METADATA_KEY_NAME, value = descriptor.moduleRevisionId.name, system = true),
            MetadataModel(key = METADATA_KEY_BRANCH, value = descriptor.moduleRevisionId.branch, system = true),
            MetadataModel(key = METADATA_KEY_REVISION, value = descriptor.moduleRevisionId.revision, system = true),
            MetadataModel(key = METADATA_KEY_ATTRIBUTES, value = descriptor.moduleRevisionId.attributes, system = true),
            MetadataModel(
                key = METADATA_KEY_EXTRA_ATTRIBUTES,
                value = descriptor.moduleRevisionId.extraAttributes,
                system = true
            ),
            MetadataModel(
                key = METADATA_KEY_QUALIFIED_EXTRA_ATTRIBUTES,
                value = descriptor.moduleRevisionId.qualifiedExtraAttributes,
                system = true
            ),
            MetadataModel(key = METADATA_KEY_PUBLISH_ARTIFACT, value = descriptor.allArtifacts, system = true),
            MetadataModel(key = METADATA_KEY_MASTER_ARTIFACT_FULL_PATH, value = masterArtifactFullPath, system = true),
            MetadataModel(key = METADATA_KEY_IVY_FULL_PATH, value = ivyFullPath, system = true),
        )
        masterArtifact?.let {
            metaDataModels.add(
                MetadataModel(key = METADATA_KEY_MASTER_ARTIFACT, value = masterArtifact, system = true),
            )
        }
        return metaDataModels
    }


    companion object {
        private val logger: Logger = LoggerFactory.getLogger(IvyLocalRepository::class.java)
    }
}

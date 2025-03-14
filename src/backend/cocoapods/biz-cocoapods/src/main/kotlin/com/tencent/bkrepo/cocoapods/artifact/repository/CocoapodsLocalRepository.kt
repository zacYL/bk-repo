/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.cocoapods.artifact.repository

import com.tencent.bkrepo.cocoapods.artifact.CocoapodsProperties
import com.tencent.bkrepo.cocoapods.constant.SPECS_FILE_CONTENT
import com.tencent.bkrepo.cocoapods.constant.SPECS_FILE_NAME
import com.tencent.bkrepo.cocoapods.exception.CocoapodsException
import com.tencent.bkrepo.cocoapods.exception.CocoapodsFileParseException
import com.tencent.bkrepo.cocoapods.exception.CocoapodsMessageCode
import com.tencent.bkrepo.cocoapods.pojo.PodSpec
import com.tencent.bkrepo.cocoapods.pojo.artifact.CocoapodsArtifactInfo
import com.tencent.bkrepo.cocoapods.service.CocoapodsPackageService
import com.tencent.bkrepo.cocoapods.utils.DecompressUtil.getPodSpec
import com.tencent.bkrepo.cocoapods.utils.PathUtil.generateCachePath
import com.tencent.bkrepo.cocoapods.utils.PathUtil.generateFullPath
import com.tencent.bkrepo.cocoapods.utils.PathUtil.generateIndexPath
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.local.LocalRepository
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.metadata.util.PackageKeys
import com.tencent.bkrepo.repository.pojo.download.PackageDownloadRecord
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CocoapodsLocalRepository(
    private val cocoapodsProperties: CocoapodsProperties,
    private val cocoapodsPackageService: CocoapodsPackageService,
) : LocalRepository() {
    override fun onUploadBefore(context: ArtifactUploadContext) {

        with(context) {
            val artifactInfo = context.artifactInfo as CocoapodsArtifactInfo

            val podSpec = getArtifactFile().getInputStream().use {
                try {
                    //只支持tar.gz格式
                    val tarFilePath = generateCachePath(artifactInfo, cocoapodsProperties.domain)
                    it.getPodSpec(tarFilePath)
                } catch (e: Exception) {
                    logger.error(
                        "projectId [$projectId],repo [$repoName] upload package [${artifactInfo.name}] " +
                                "error, ${e.message}"
                    )
                    throw CocoapodsFileParseException(CocoapodsMessageCode.COCOAPODS_FILE_PARSE_ERROR)
                }
            }
            validateNameAndVersion(podSpec, artifactInfo)
            putAttribute(SPECS_FILE_NAME, podSpec.fileName)
            putAttribute(SPECS_FILE_CONTENT, podSpec.content)
        }
        with(context.artifactInfo as CocoapodsArtifactInfo) {
            packageService
                .findVersionByName(projectId, repoName, PackageKeys.ofCocoapods(name), version)
                ?.apply { uploadIntercept(context, this) }
        }

        super.onUploadBefore(context)
    }

    private fun validateNameAndVersion(podSpec: PodSpec, artifactInfo: CocoapodsArtifactInfo) {
        // 校验包名与spec内容的name要一致
        if (podSpec.name == null) {
            throw CocoapodsException("podspec name cannot be empty.")
        }
        if (artifactInfo.name != podSpec.name) {
            throw CocoapodsException("package name should be consistent with the name attribute in the podspec file.")
        }
        if (podSpec.version == null) {
            throw CocoapodsException("podspec version cannot be empty.")
        }
        if (artifactInfo.version != podSpec.version) {
            throw CocoapodsException(
                "package version should be consistent with the version attribute in the podspec file."
            )
        }
    }

    override fun onUploadSuccess(context: ArtifactUploadContext) {

        with(context) {
            val artifactInfo = artifactInfo as CocoapodsArtifactInfo
            //在.specs目录创建索引文件
            val fileName = getAttribute<String>(SPECS_FILE_NAME)
            val podSpec = getAttribute<String>(SPECS_FILE_CONTENT)
                ?: throw CocoapodsFileParseException(CocoapodsMessageCode.COCOAPODS_FILE_PARSE_ERROR)
            ByteArrayOutputStream().use { bos ->
                OutputStreamWriter(bos, Charsets.UTF_8).use { writer ->
                    writer.write(podSpec)
                }
                val specArtifact = ArtifactFileFactory.build(bos.toByteArray().inputStream())
                val uploadContext = ArtifactUploadContext(specArtifact)
                val specNode = buildNodeCreateRequest(uploadContext).run {
                    copy(fullPath = generateIndexPath(artifactInfo, fileName))
                }
                storageManager.storeArtifactFile(specNode, specArtifact, uploadContext.storageCredentials)
            }
            //创建包版本
            cocoapodsPackageService.createVersion(artifactInfo, getArtifactFile().getSize())
        }
        super.onUploadSuccess(context)
    }

    override fun buildNodeCreateRequest(context: ArtifactUploadContext): NodeCreateRequest {
        with(context) {
            val cocoapodsArtifactInfo = artifactInfo as CocoapodsArtifactInfo
            val fullPath = generateFullPath(cocoapodsArtifactInfo)
            logger.info("File $fullPath will be stored in $projectId|$repoName")
            return super.buildNodeCreateRequest(context).copy(
                fullPath = fullPath,
                nodeMetadata = cocoapodsArtifactInfo.generateMetadata(),
                overwrite = true
            )
        }
    }

    override fun buildDownloadRecord(
        context: ArtifactDownloadContext,
        artifactResource: ArtifactResource,
    ): PackageDownloadRecord? {
        val artifactInfo = context.artifactInfo
        return if (artifactInfo is CocoapodsArtifactInfo) {
            cocoapodsPackageService.buildDownloadRecord(artifactInfo, context.userId)
        } else null
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CocoapodsLocalRepository::class.java)
    }
}


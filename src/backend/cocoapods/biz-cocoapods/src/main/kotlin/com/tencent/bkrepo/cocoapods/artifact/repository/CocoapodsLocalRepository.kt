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
import com.tencent.bkrepo.cocoapods.constant.CocoapodsProperties
import com.tencent.bkrepo.cocoapods.constant.DOT_SPECS
import com.tencent.bkrepo.cocoapods.pojo.artifact.CocoapodsArtifactInfo
import com.tencent.bkrepo.cocoapods.service.CocoapodsPackageService
import com.tencent.bkrepo.cocoapods.utils.DecompressUtil.getPodSpec
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.local.LocalRepository
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.util.PackageKeys
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter

@Component
class CocoapodsLocalRepository(
    private val cocoapodsProperties: CocoapodsProperties,
    private val cocoapodsPackageService: CocoapodsPackageService
) : LocalRepository() {
    override fun onUploadBefore(context: ArtifactUploadContext) {
        //todo 校验文件
        super.onUploadBefore(context)
        with(context.artifactInfo as CocoapodsArtifactInfo) {
            packageClient
                .findVersionByName(projectId, repoName, PackageKeys.ofCocoapods(name), version).data
                ?.apply { uploadIntercept(context, this) }
        }
    }

    override fun onUploadSuccess(context: ArtifactUploadContext) {

        with(context) {
            val artifactInfo = artifactInfo as CocoapodsArtifactInfo
            //在.specs目录创建索引文件
            getArtifactFile().getInputStream().use {
                val tarFilePath = "${cocoapodsProperties.domain}/${projectId}/${repoName}/${artifactInfo.getArtifactFullPath()}"
                val (fileName, podSpec) = it.getPodSpec(tarFilePath)
                ByteArrayOutputStream().use { bos ->
                    OutputStreamWriter(bos, Charsets.UTF_8).use { writer ->
                        writer.write(podSpec)
                    }
                    val specArtifact = ArtifactFileFactory.build(bos.toByteArray().inputStream())
                    val uploadContext = ArtifactUploadContext(specArtifact)
                    val specNode = buildNodeCreateRequest(uploadContext).run {
                        copy(fullPath = "$DOT_SPECS/${artifactInfo.name}/${artifactInfo.version}/${fileName}")
                    }
                    storageManager.storeArtifactFile(specNode, specArtifact, uploadContext.storageCredentials)
                }
            }
            //创建包版本
            cocoapodsPackageService.createVersion(artifactInfo, getArtifactFile().getSize())
        }
        super.onUploadSuccess(context)
    }


}

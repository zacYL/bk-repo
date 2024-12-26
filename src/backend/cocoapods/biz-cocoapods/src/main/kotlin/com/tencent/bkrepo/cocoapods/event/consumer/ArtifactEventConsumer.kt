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

package com.tencent.bkrepo.cocoapods.event.consumer

import com.google.gson.JsonParser
import com.tencent.bkrepo.cocoapods.artifact.CocoapodsProperties
import com.tencent.bkrepo.cocoapods.pojo.enums.PodSpecType
import com.tencent.bkrepo.cocoapods.service.CocoapodsWebService
import com.tencent.bkrepo.cocoapods.utils.CocoapodsUtil
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.event.base.ArtifactEvent
import com.tencent.bkrepo.common.artifact.event.base.EventType
import com.tencent.bkrepo.common.artifact.manager.StorageManager
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.stream.ArtifactInputStream
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.Message
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.util.function.Consumer

@Component("artifactEvent")
class ArtifactEventConsumer(
    private val nodeClient: NodeClient,
    private val repoClient: RepositoryClient,
    private val cocoapodsProperties: CocoapodsProperties
) : Consumer<Message<ArtifactEvent>> {

    @Autowired
    lateinit var storageManager: StorageManager

    override fun accept(message: Message<ArtifactEvent>) {
        val event = message.payload
        handle(event)
    }

    private fun handle(event: ArtifactEvent) {
        when (event.type) {
            EventType.COCOAPODS_REPLICA -> resolveIndexFile(event)
            else -> {}
        }
    }

    /**
     * 制品分发后，接受制品方需要修改索引文件
     * {fullPath}.podspec或者{fullPath}.podspec.json
     */
    private fun resolveIndexFile(event: ArtifactEvent) {
        logger.info("Cocoapods-Event: resolveIndexFile...")
        with(event) {

            val packageFilePath = data["contentPath"] as (String)

            val indexFilePath = getIndexFilePath(projectId, repoName, packageFilePath)

            val type = checkIndexFilePath(indexFilePath)

            if (type == 0) return

            val repoDetail = repoClient
                .getRepoDetail(projectId, repoName)
                .data as RepositoryDetail
            val nodeDetail = nodeClient
                .getNodeDetail(projectId, repoName, indexFilePath)
                .data as NodeDetail
            //索引源文件InputStream
            val indexFileInputStream =
                storageManager.loadArtifactInputStream(nodeDetail, repoDetail.storageCredentials) ?: return

            //目标地址,ex:"http://bkrepo.indecpack7.com/cocoapods/z153ce/hb-pod-1220//MatthewYork/DateTools/5.0.0/DateTools-5.0.0.tar.gz"
            val sourcePath = "${cocoapodsProperties.domain}/${projectId}/${repoName}//${packageFilePath}"

            when (type) {
                1 -> handleForPodSpec(indexFileInputStream, sourcePath, repoDetail, indexFilePath)
                2 -> handleForPodSpecJson(indexFileInputStream, sourcePath, repoDetail, indexFilePath)
                else -> {}
            }
        }
    }

    /**
     * 处理podspec.json格式
     * 替换source下的地址为本地节点地址
     * 并存储文件
     */
    private fun handleForPodSpecJson(
        inputStream: ArtifactInputStream,
        sourcePath: String,
        repoDetail: RepositoryDetail,
        indexFilePath: String
    ) {
        val jsonStr = JsonParser.parseReader(InputStreamReader(inputStream)).toJsonString()
        val newJsonStr = CocoapodsUtil.updatePodspecJsonSource(jsonStr, sourcePath)
        val newInputStream = ByteArrayInputStream(newJsonStr.toByteArray())
        val artifactFile = ArtifactFileFactory.build(newInputStream)
        store(artifactFile, repoDetail, indexFilePath)
    }

    /**
     * 存储文件
     */
    private fun store(artifactFile: ArtifactFile, repoDetail: RepositoryDetail, indexFilePath: String) {
        with(repoDetail) {
            val nodeCreateRequest = NodeCreateRequest(projectId, name, indexFilePath, false)
            storageManager.storeArtifactFile(nodeCreateRequest, artifactFile, storageCredentials)
        }
    }

    /**
     * 处理podspec格式
     * 替换source下的地址为本地节点地址
     * 并存储文件
     */
    private fun handleForPodSpec(
        inputStream: ArtifactInputStream,
        sourcePath: String,
        repoDetail: RepositoryDetail,
        indexFilePath: String
    ) {
        val specStr = getSpecStr(inputStream)
        val newSpecStr = CocoapodsUtil.updatePodspecSource(specStr, sourcePath)
        val newInputStream = ByteArrayInputStream(newSpecStr.toByteArray())
        val artifactFile = ArtifactFileFactory.build(newInputStream)
        store(artifactFile, repoDetail, indexFilePath)
    }

    /**
     * 检查是否是索引文件，返回索引类型
     * 不是索引文件返回0
     * podspec格式返回1
     * podspec.json返回2
     */
    private fun checkIndexFilePath(path: String): Int {
        if (path.isEmpty()) return 0
        if (path.endsWith(PodSpecType.POD_SPEC.extendedName)) return 1
        if (path.endsWith(PodSpecType.JSON.extendedName)) return 2
        return 0
    }

    /**
     * 根据制品文件路径，构造索引文件路径
     * 查找是否存在并返回
     */
    private fun getIndexFilePath(projectId: String, repoName: String, packageFilePath: String): String {
        val split = packageFilePath.split('/')
        val orgName = split[0]
        val artifactName = split[1]
        val versionName = split[2]
        val specsPath = "/.specs/${artifactName}/${versionName}/${artifactName}.podspec"
        val jsonPath = "/.specs/${artifactName}/${versionName}/${artifactName}.podspec.json"
        val pathList: List<String> =
            nodeClient.listExistFullPath(projectId, repoName, listOf(specsPath, jsonPath)).data.orEmpty()
        if (pathList.isEmpty()) return ""
        return pathList[0]
    }

    /**
     * 将 InputStream 转换为字符串
     * @param inputStream 输入流
     * @return 转换后的字符串
     */
    fun getSpecStr(inputStream: InputStream): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        inputStream.use { input ->
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead)
            }
        }
        return byteArrayOutputStream.toString(Charsets.UTF_8.name())
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CocoapodsWebService::class.java)
    }
}
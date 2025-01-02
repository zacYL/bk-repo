package com.tencent.bkrepo.cocoapods.service

import com.google.gson.JsonParser
import com.tencent.bkrepo.cocoapods.artifact.CocoapodsProperties
import com.tencent.bkrepo.cocoapods.pojo.enums.PodSpecType
import com.tencent.bkrepo.cocoapods.utils.CocoapodsUtil
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.event.base.ArtifactEvent
import com.tencent.bkrepo.common.artifact.manager.StorageManager
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.stream.ArtifactInputStream
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.storage.filesystem.FileSystemClient
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.pojo.node.service.NodeDeleteRequest
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.math.log

@Service
class CocoapodsReplicaService(
    private val nodeClient: NodeClient,
    private val repoClient: RepositoryClient,
    private val storageManager: StorageManager
) {

    /**
     * 制品分发后，接受制品方需要修改索引文件
     * {fullPath}.podspec或者{fullPath}.podspec.json
     */
    fun resolveIndexFile(event: ArtifactEvent) {
        logger.info("Cocoapods-Event: resolveIndexFile, event:[$event]")
        with(event) {

            val packageFilePath = data["fullPath"] as? String?:
            throw IllegalArgumentException("fullPath is missing or not a string")

            val indexFilePath = getIndexFilePath(projectId, repoName, packageFilePath)

            val type = checkIndexFilePath(indexFilePath)
            logger.info("packageFilePath: $packageFilePath\nindexFilePath: $indexFilePath\ntype: $type")

            if (type == 0) return

            val repoDetail = repoClient
                .getRepoDetail(projectId, repoName)
                .data as RepositoryDetail
            val nodeDetail = nodeClient
                .getNodeDetail(projectId, repoName, indexFilePath)
                .data as NodeDetail

            logger.info("repoDetail: $repoDetail")
            logger.info("nodeDetail: $nodeDetail")

            //索引源文件InputStream
            val indexFileInputStream =
                storageManager.loadArtifactInputStream(nodeDetail, repoDetail.storageCredentials) ?: return
            val domain = data["domain"] as? String?:
            throw IllegalArgumentException("domain is missing or not a string")

            //目标地址,ex:"http://bkrepo.indecpack7.com/cocoapods/z153ce/hb-pod-1220//MatthewYork/DateTools/5.0.0/DateTools-5.0.0.tar.gz"
            val sourcePath = "${domain}/${projectId}/${repoName}/${packageFilePath}"

            logger.info("replace with sourcePath: $sourcePath")

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
        logger.info("newJsonStr: $newJsonStr")
        val newInputStream = ByteArrayInputStream(newJsonStr.toByteArray())
        val artifactFile = ArtifactFileFactory.build(newInputStream)
        store(artifactFile, repoDetail, indexFilePath)
    }

    /**
     * 存储文件
     */
    private fun store(artifactFile: ArtifactFile, repoDetail: RepositoryDetail, indexFilePath: String) {
        logger.info("start to store $indexFilePath")
        with(repoDetail) {
            val nodeCreateRequest = NodeCreateRequest(
                projectId,
                name,
                indexFilePath,
                false,
                sha256 = artifactFile.getFileSha256(),
                md5 = artifactFile.getFileMd5(),
                overwrite = true
            )
            val nodeDetail =
                storageManager.storeArtifactFile(nodeCreateRequest, artifactFile, storageCredentials)
            logger.info("the new nodeDetail is: $nodeDetail")
        }
        logger.info("store success")
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
        logger.info("newSpecStr: $newSpecStr")
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
        val orgName = split[1]
        val artifactName = split[2]
        val versionName = split[3]
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
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}

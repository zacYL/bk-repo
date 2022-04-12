package com.tencent.bkrepo.executor.model

import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.storage.core.StorageService
import com.tencent.bkrepo.executor.config.ExecutorConfig
import com.tencent.bkrepo.executor.exception.BuildWorkSpaceFailedException
import com.tencent.bkrepo.executor.exception.LoadConfigFileFailedException
import com.tencent.bkrepo.executor.pojo.context.FileScanContext
import com.tencent.bkrepo.repository.api.NodeClient
import com.tencent.bkrepo.repository.api.RepositoryClient
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.expression.common.TemplateParserContext
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.stereotype.Component
import java.io.File

@Component
class HostRunTime @Autowired constructor(
    private val nodeClient: NodeClient,
    private val storageService: StorageService,
    private val repositoryClient: RepositoryClient
) {

    /**
     * 生成工作空间
     * @param workDir  工作目录
     * @throws BuildWorkSpaceFailedException
     */
    fun buildWorkSpace(workDir: File) {
        // 清理生成workspace
        if (!cleanWorkSpace(workDir) || !workDir.mkdirs()) {
            throw BuildWorkSpaceFailedException("build work space failed")
        }
    }

    /**
     * 清理工作空间
     * @param workDir  工作目录
     */
    fun cleanWorkSpace(workDir: File): Boolean {
        return workDir.deleteRecursively()
    }

    /**
     * 加载配置文件
     * @param taskId  任务ID
     * @param workDir 工作目录
     * @param config 配置文件目录
     * @param sha256 需要扫描文件的sha256
     * @return Boolean
     */
    fun loadConfigFile(taskId: String, workDir: File, config: ExecutorConfig, sha256: String): Boolean {
        try {
            val template = File(config.configTemplateDir).readText()
            val params = mapOf(
                "taskId" to taskId,
                "sha256" to sha256
            )

            val content = SpelExpressionParser()
                .parseExpression(template, TemplateParserContext())
                .getValue(params, String::class.java)
                ?: return false

            File(workDir, config.configName).writeText(content)
            return true
        } catch (e: Exception) {
            logger.warn("load config file exception [$taskId,$e] ")
            throw LoadConfigFileFailedException("load config file exception")
        }
    }

    /**
     * 加载待扫描文件
     * @param context  扫描文件contexnt
     * @param workDir 工作目录
     * @return String?
     */
    fun loadFileToRunTime(context: FileScanContext, workDir: File): String? {
        with(context) {
            try {
                //  load file
                val repository = repositoryClient.getRepoDetail(projectId, repoName).data
                if (repository == null) {
                    logger.warn("fail to get the repo [$context]")
                    return null
                }
                val node = nodeClient.getNodeDetail(projectId, repoName, fullPath).data
                if (node == null) {
                    logger.warn("fail to get the node [$context]")
                    return null
                }
                val inputDir = File(workDir, context.config.inputDir)
                val file = File(inputDir, node.sha256!!)
                val inputStream = storageService.load(
                    node.sha256!!, Range.full(node.size),
                    repository.storageCredentials
                )
                if (inputStream == null) {
                    logger.warn("fail to get the input stream [$context]")
                    return null
                }
                inputStream.use {
                    FileUtils.copyInputStreamToFile(inputStream, file)
                }
                return node.sha256
            } catch (e: Exception) {
                logger.warn("load file to runtime exception [$e] ")
                return null
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(HostRunTime::class.java)
    }
}

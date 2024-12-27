package com.tencent.bkrepo.cocoapods.utils

import org.eclipse.jgit.api.Git
import org.slf4j.LoggerFactory
import java.io.File

object GitUtil {
    /**
     * @param repoUrl Git 仓库地址
     * @param localPath 本地存储路径
     */
    fun cloneOrPullRepo(repoUrl: String, localPath: String): String {
        val repoDir = File(localPath)
        logger.info("git url: $repoUrl, localPath: $localPath")

        try {
            if (repoDir.exists() && repoDir.isDirectory) {
                // 如果目录已存在，尝试执行 `git pull`
                logger.info("Local repository exists. Performing 'git pull'.")
                Git.open(repoDir).use { git ->
                    git.pull().call()
                }
                logger.info("Repository $ pull successfully.")
            } else {
                // 如果目录不存在，执行 `git clone`
                logger.info("Cloning repository from $repoUrl to $localPath.")
                Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(repoDir)
                    .call()
            }
            logger.info("Repository updated successfully.")
            return getLatestRefs(localPath)
        } catch (e: Exception) {
            logger.error("Failed to update repository: ${e.message}", e)
            throw e
        }
    }

    fun getLatestRefs(repoPath: String): String {
        // 打开本地仓库
        val git = Git.open(File(repoPath))

        try {
            // 获取最新的提交哈希
            val repository = git.repository
            val headRef = repository.findRef("HEAD") // 获取 HEAD 引用
            val latestCommit = repository.resolve(headRef.objectId.name) // 获取提交 ID
            println("Latest commit hash: $latestCommit")
            return latestCommit.name
        }catch (e: Exception){
            logger.error("Failed to get latest refs: ${e.message}", e)
            throw e
        }
    }
    private val logger = LoggerFactory.getLogger(this::class.java)
}

package com.tencent.bkrepo.cocoapods.utils

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.CredentialsProvider
import org.slf4j.LoggerFactory
import java.io.File

object GitUtil {
    /**
     * @param repoUrl Git 仓库地址
     * @param localPath 本地存储路径
     */
    fun cloneOrPullRepo(repoUrl: String, localPath: String, credentialsProvider: CredentialsProvider?): String {
        val repoDir = File(localPath)
        logger.info("Git URL: $repoUrl, Local Path: $localPath")

        try {
            if (repoDir.exists() && repoDir.isDirectory) {
                // 如果目录已存在，尝试执行 `git pull`
                logger.info("Local repository exists. Performing 'git pull'.")
                Git.open(repoDir).use { git ->
                    val pullCommand = git.pull()
                    if (credentialsProvider != null) {
                        pullCommand.setCredentialsProvider(credentialsProvider)
                    }
                    pullCommand.call()
                }
                logger.info("Repository pulled successfully.")
            } else {
                // 如果目录不存在，执行 `git clone`
                logger.info("Cloning repository from $repoUrl to $localPath.")
                val cloneCommand = Git.cloneRepository()
                    .setURI(repoUrl)
                    .setDirectory(repoDir)
                    .setNoTags()
                    .setBranchesToClone(listOf("refs/heads/master")) // 仅克隆指定分支
                    .setBranch("refs/heads/master") // 设置克隆的起始分支
                if (credentialsProvider != null) {
                    cloneCommand.setCredentialsProvider(credentialsProvider)
                }
                cloneCommand.call()
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

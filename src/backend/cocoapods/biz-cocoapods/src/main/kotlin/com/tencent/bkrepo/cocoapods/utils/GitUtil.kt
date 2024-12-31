package com.tencent.bkrepo.cocoapods.utils

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.TagOpt
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
                // 如果目录已存在，尝试执行 `git fetch` + `reset`
                logger.info("Local repository exists. Fetching updates from remote.")

                // 检测并删除锁文件
                val lockFile = File(repoDir, ".git/index.lock")
                if (lockFile.exists()) {
                    logger.warn("Lock file detected: ${lockFile.absolutePath}. Deleting it to prevent conflicts.")
                    lockFile.delete()
                }

                Git.open(repoDir).use { git ->
                    // 执行 fetch，启用浅拉取和禁用标签
                    val fetchCommand = git.fetch()
                        .setRemote("origin") // 远程仓库名
                        .setRefSpecs(RefSpec("refs/heads/master:refs/heads/master")) // 拉取目标分支
                        .setTagOpt(TagOpt.NO_TAGS) // 禁用标签拉取
                    if (credentialsProvider != null) {
                        fetchCommand.setCredentialsProvider(credentialsProvider)
                    }
                    fetchCommand.call()
                    logger.info("Fetch completed.")

                    // 执行 reset，将本地分支同步到远程分支
                    git.reset()
                        .setMode(ResetCommand.ResetType.HARD)
                        .setRef("refs/remotes/origin/master") // 指定远程分支
                        .call()
                    logger.info("Repository reset to remote state.")
                }
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
            val latestRefs = getLatestRefs(localPath)
            logger.info("Latest Refs: $latestRefs")
            return latestRefs
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
            logger.info("Latest commit hash: $latestCommit")
            return latestCommit.name
        }catch (e: Exception){
            logger.error("Failed to get latest refs: ${e.message}", e)
            throw e
        }
    }
    private val logger = LoggerFactory.getLogger(this::class.java)
}

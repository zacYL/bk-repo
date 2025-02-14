package com.tencent.bkrepo.cocoapods.service

import com.tencent.bkrepo.common.artifact.exception.RepoNotFoundException
import com.tencent.bkrepo.common.artifact.pojo.configuration.RepositoryConfiguration
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.constant.SYSTEM_USER
import com.tencent.bkrepo.repository.pojo.repo.RepoUpdateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepositoryInfo
import org.springframework.stereotype.Service

@Service
class CocoapodsRepoService(
    private val repositoryClient: RepositoryClient,
) {

    fun getRepoInfo(projectId: String, repoName: String): RepositoryInfo {
        return repositoryClient.getRepoInfo(projectId, repoName).data ?: throw RepoNotFoundException(repoName)
    }

    fun getStringSetting(projectId: String, repoName: String, key: String): String? {
        return getRepoInfo(projectId, repoName).configuration.getStringSetting(key)
    }

    fun updateStringSetting(projectId: String, repoName: String, key: String, value: String) {
        val repoInfo = getRepoInfo(projectId, repoName)
        repoInfo.configuration.settings[key] = value
        repositoryClient.updateRepo(createRepoUpdateRequest(projectId, repoName, repoInfo, repoInfo.configuration))
    }

    private fun createRepoUpdateRequest(
        projectId: String,
        repoName: String,
        repoInfo: RepositoryInfo,
        configuration: RepositoryConfiguration,
    ): RepoUpdateRequest {
        return RepoUpdateRequest(
            projectId = projectId,
            name = repoName,
            public = repoInfo.public,
            description = repoInfo.description,
            configuration = configuration,
            operator = SYSTEM_USER
        )
    }
}

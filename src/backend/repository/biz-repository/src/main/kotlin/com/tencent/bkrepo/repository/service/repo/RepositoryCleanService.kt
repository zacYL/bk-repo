package com.tencent.bkrepo.repository.service.repo

import com.tencent.bkrepo.common.artifact.pojo.configuration.clean.RepositoryCleanStrategy

/**
 * 仓库清理接口
 */
interface RepositoryCleanService {
    /**
     * 清理仓库
     * @param repoId 仓库id
     */
    fun cleanRepo(repoId: String)
}

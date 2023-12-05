package com.tencent.bkrepo.repository.service.repo

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.repository.pojo.storage.RepoLogicStoragePojo
import com.tencent.bkrepo.repository.pojo.storage.RepoStorageInfoParam
import com.tencent.bkrepo.repository.pojo.storage.StoragePojo

interface StorageManageService {
    /**
     * 查看磁盘信息
     */
    fun info(): StoragePojo

    /**
     * 查看项目仓库存储信息
     */
    fun infoRepos(
        repoStorageInfoParam: RepoStorageInfoParam
    ): Page<RepoLogicStoragePojo>

    /**
     * 更新项目仓库存储信息redis缓存
     */
    fun updateRepoStorageCache(): List<RepoLogicStoragePojo>
}

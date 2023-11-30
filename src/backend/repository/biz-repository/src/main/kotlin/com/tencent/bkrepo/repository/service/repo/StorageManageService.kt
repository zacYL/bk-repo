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

    fun infoRepos(
        repoStorageInfoParam: RepoStorageInfoParam
    ): Page<RepoLogicStoragePojo>
}

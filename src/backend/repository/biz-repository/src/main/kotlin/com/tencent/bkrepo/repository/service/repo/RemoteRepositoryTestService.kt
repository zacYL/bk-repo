package com.tencent.bkrepo.repository.service.repo

import com.tencent.bkrepo.repository.pojo.repo.ConnectionStatusInfo
import com.tencent.bkrepo.repository.pojo.repo.RemoteUrlRequest

interface RemoteRepositoryTestService {
    fun testRemoteUrl(remoteUrlRequest: RemoteUrlRequest): ConnectionStatusInfo
}

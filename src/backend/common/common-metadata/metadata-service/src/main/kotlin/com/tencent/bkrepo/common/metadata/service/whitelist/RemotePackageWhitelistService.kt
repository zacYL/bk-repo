package com.tencent.bkrepo.common.metadata.service.whitelist

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.repository.pojo.whitelist.CreateRemotePackageWhitelistRequest
import com.tencent.bkrepo.repository.pojo.whitelist.RemotePackageWhitelist
import com.tencent.bkrepo.repository.pojo.whitelist.UpdateRemotePackageWhitelistRequest

interface RemotePackageWhitelistService {
    fun createWhitelist(request: CreateRemotePackageWhitelistRequest): Boolean

    fun updateWhitelist(id: String, request: UpdateRemotePackageWhitelistRequest): Boolean

    fun deleteWhitelist(id: String): Boolean

    fun getWhitelist(id: String): RemotePackageWhitelist?

    fun page(
            type: RepositoryType?,
            packageKey: String?,
            version: String?,
            pageNumber: Int?,
            pageSize: Int?,
            regex: Boolean
    ): Page<RemotePackageWhitelist>

    fun existWhitelist(type: RepositoryType?, packageKey: String?, version: String?, ): Boolean

    fun batchWhitelist(request: List<CreateRemotePackageWhitelistRequest>): Int
}

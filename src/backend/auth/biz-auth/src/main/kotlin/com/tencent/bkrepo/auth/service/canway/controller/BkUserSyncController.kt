package com.tencent.bkrepo.auth.service.canway.controller

import com.tencent.bkrepo.auth.service.canway.api.BkUserSyncApi
import com.tencent.bkrepo.auth.service.canway.bk.BkUserService

class BkUserSyncController(
    private val bkUserService: BkUserService
) : BkUserSyncApi {
    override fun syncBkUser() {
        bkUserService.syncBkUser()
    }
}

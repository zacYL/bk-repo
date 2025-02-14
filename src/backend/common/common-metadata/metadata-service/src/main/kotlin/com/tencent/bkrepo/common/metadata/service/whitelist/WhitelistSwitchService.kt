package com.tencent.bkrepo.common.metadata.service.whitelist

import com.tencent.bkrepo.common.artifact.pojo.RepositoryType

interface WhitelistSwitchService {

    fun list(): Map<RepositoryType, Boolean>

    fun switch(type: RepositoryType, status: Boolean?): Boolean

    fun get(type: RepositoryType): Boolean

}

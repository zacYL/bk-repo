package com.tencent.bkrepo.common.metadata.service.node

import com.tencent.bkrepo.repository.pojo.node.UserAuthPathOption

interface NodePermissionService {
    fun getUserAuthPathCache(option: UserAuthPathOption): Map<String, List<String>>
}

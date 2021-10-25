package com.tencent.bkrepo.common.devops.api.pojo.request

import com.tencent.bkrepo.common.devops.BELONGCODE
import com.tencent.bkrepo.common.devops.RESOURCECODE
import com.tencent.bkrepo.common.devops.api.enums.CanwayPermissionType

data class CanwayPermissionRequest(
    val userId: String,
    val belongCode: String? = BELONGCODE,
    val belongInstance: String,
    val resourcesActions: Set<CanwayAction>
) {
    data class CanwayAction(
        val actionCode: CanwayPermissionType,
        val resourceCode: String? = RESOURCECODE,
        val resourceInstance: Set<CanwayInstance>
    ) {
        data class CanwayInstance(
            val resourceCode: String? = RESOURCECODE,
            val instanceCode: String = ""
        )
    }
}

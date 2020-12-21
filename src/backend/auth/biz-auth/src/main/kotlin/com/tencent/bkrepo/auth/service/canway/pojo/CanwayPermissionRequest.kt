package com.tencent.bkrepo.auth.service.canway.pojo

import com.tencent.bkrepo.auth.service.canway.BELONGCODE
import com.tencent.bkrepo.auth.service.canway.RESOURCECODE

data class CanwayPermissionRequest(
    val userId: String,
    val belongCode: String? = BELONGCODE,
    val belongInstance: String,
    val resourcesActions: Set<CanwayAction>
) {
    data class CanwayAction(
        val actionCode: String,
        val resourceCode: String? = RESOURCECODE,
        val resourceInstance: Set<CanwayInstance>
    ) {
        data class CanwayInstance(
            val resourceCode: String? = RESOURCECODE,
            val instanceCode: String = ""
        )
    }
}

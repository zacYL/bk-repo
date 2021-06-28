package com.tencent.bkrepo.auth.listener.event.user

import com.tencent.bkrepo.repository.pojo.log.OperateType

class UserDeletedEvent(override val userIdData: String, override val operator: String) : UserEvent(
    userIdData = userIdData,
    nameData = null,
    operator = operator
) {
    override fun getOperateType() = OperateType.DELETE
}

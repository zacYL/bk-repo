package com.tencent.bkrepo.auth.listener.event.user

import com.tencent.bkrepo.auth.pojo.user.CreateUserRequest
import com.tencent.bkrepo.repository.pojo.log.OperateType

class UserCreateEvent(val request: CreateUserRequest, override val operator: String) : UserEvent(
    userIdData = request.userId,
    nameData = request.name,
    operator = operator
) {
    override fun getOperateType() = OperateType.CREATE
}

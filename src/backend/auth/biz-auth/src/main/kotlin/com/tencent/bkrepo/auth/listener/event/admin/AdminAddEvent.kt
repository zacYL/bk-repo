package com.tencent.bkrepo.auth.listener.event.admin

import com.tencent.bkrepo.repository.pojo.log.OperateType

class AdminAddEvent(
    override val list: List<String>,
    override val operator: String
) : AdminEvent(list, operator) {
    override fun getOperateType() = OperateType.CREATE
}

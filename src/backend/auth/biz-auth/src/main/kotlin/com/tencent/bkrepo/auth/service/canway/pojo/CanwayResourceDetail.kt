package com.tencent.bkrepo.auth.service.canway.pojo

data class CanwayResourceDetail(
    val id: String,
    val code: String,
    val name: String,
    val parentCode: String,
    val functionList: List<CanwayFunction>
) {
    data class CanwayFunction(
        val id: String,
        val name: String,
        val action: CanwayAction,
        val associatedInstance: Boolean
    )
}

data class CanwayAction(
    val id: String,
    val code: String,
    val name: String,
    val createUser: String,
    val createTime: Long,
    val updateUser: String,
    val updateTime: Long,
    val associatedInstance: Boolean
)

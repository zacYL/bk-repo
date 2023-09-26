package com.tencent.bkrepo.repository.pojo.metadata

data class LimitData(
    val limitStatus: String,
    val limitUser: String,
    val limitActionType: String,
    val limitAction: String
)

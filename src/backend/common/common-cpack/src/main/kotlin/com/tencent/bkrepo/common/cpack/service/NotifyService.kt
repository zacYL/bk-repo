package com.tencent.bkrepo.common.cpack.service

interface NotifyService {

    fun fileShare(userId: String, content: String)

    fun fileShare(userId: String, content: String, users: List<String>)

    fun newAccountMessage(userId: String, cnName: String, receivers: List<String>)

    fun resetPwdMessage(userId: String, cnName: String, receivers: List<String>)
}

package com.tencent.bkrepo.common.cpack.service

interface NotifyService {

    fun fileShare(userId: String, content: String)

    fun fileShare(userId: String, content: String, users: List<String>)
}

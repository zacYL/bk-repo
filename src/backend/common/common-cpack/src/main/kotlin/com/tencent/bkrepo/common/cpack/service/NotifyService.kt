package com.tencent.bkrepo.common.cpack.service

interface NotifyService {

    fun fileShare(userId: String, content: String)
}

package com.tencent.bkrepo.auth.service

import com.tencent.bkrepo.auth.pojo.CanwayBkrepoInstance


interface CanwayAuthService {
    fun instanceld(projectId: String): List<CanwayBkrepoInstance>
}
package com.tencent.bkrepo.auth.controller.service

import com.tencent.bkrepo.auth.api.CanwayAuthClient
import com.tencent.bkrepo.auth.pojo.CanwayBkrepoInstance
import com.tencent.bkrepo.auth.service.CanwayAuthService
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import org.springframework.web.bind.annotation.RestController

/**
 * auth服务提供给平台接口
 */
@RestController
class CanwayAuthController(
    private var canwayAuthService: CanwayAuthService
) : CanwayAuthClient {
    override fun instanceld(projectId: String): Response<List<CanwayBkrepoInstance>> {
        return ResponseBuilder.success(canwayAuthService.instanceld(projectId))
    }
}
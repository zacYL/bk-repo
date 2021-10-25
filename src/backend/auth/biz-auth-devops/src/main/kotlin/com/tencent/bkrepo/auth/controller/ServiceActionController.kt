package com.tencent.bkrepo.auth.controller

import com.tencent.bkrepo.auth.api.service.ServiceActionApi
import com.tencent.bkrepo.auth.pojo.CanwayAction
import com.tencent.bkrepo.auth.service.impl.ActionService
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import org.springframework.web.bind.annotation.RestController

@RestController
class ServiceActionController(
    val actionService: ActionService
) : ServiceActionApi {
    override fun listAction(project: String, repo: String): Response<Set<CanwayAction>> {
        return ResponseBuilder.success(actionService.listActions(repo))
    }
}

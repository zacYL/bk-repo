package com.tencent.bkrepo.auth.service.canway.controller

import com.tencent.bkrepo.auth.service.canway.api.ServiceActionApi
import com.tencent.bkrepo.auth.service.canway.pojo.CanwayAction
import com.tencent.bkrepo.auth.service.canway.service.ActionService
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import org.springframework.web.bind.annotation.RestController

@RestController
class ServiceActionController(
    val actionService: ActionService
) : ServiceActionApi {
    override fun listAction(project: String, repo: String): Response<Set<CanwayAction>> {
        return ResponseBuilder.success(actionService.listActions(project, repo))
    }
}

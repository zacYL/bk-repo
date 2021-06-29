package com.tencent.bkrepo.repository.controller.user

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.BK_SOFTWARE
import com.tencent.bkrepo.repository.pojo.operate.OperateLogPojo
import com.tencent.bkrepo.repository.pojo.log.ResourceType
import com.tencent.bkrepo.repository.pojo.operate.OperateLogResponse
import com.tencent.bkrepo.repository.service.operate.OperateLogService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/operate/log")
class UserOperateLogController(
    private val operateLogService: OperateLogService
) {

    @ApiOperation("审计日志查询接口")
    @GetMapping("/page")
    fun page(
        @ApiParam("资源类型", required = false)
        @RequestParam type: ResourceType?,
        @ApiParam("项目名", required = false, defaultValue = "bksoftware")
        @RequestParam projectId: String? = BK_SOFTWARE,
        @ApiParam("仓库名", required = false)
        @RequestParam repoName: String?,
        @ApiParam("页数", required = false, defaultValue = "1")
        @RequestParam number: Int?,
        @ApiParam("每页数量", required = false, defaultValue = "1")
        @RequestParam size: Int?
    ): Response<Page<OperateLogResponse>> {
        val page = operateLogService.page(type, projectId, repoName, number ?: 1, size ?: 20)
        return ResponseBuilder.success(page)
    }
}

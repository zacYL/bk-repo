package com.tencent.bkrepo.repository.controller.user

import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.pojo.log.OperateLogResponse
import com.tencent.bkrepo.repository.service.log.OperateLogService
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/operate/log")
class UserOperateLogController(
    private val operateLogService: OperateLogService
) {

    @ApiOperation("审计日志查询接口")
    @GetMapping("/page")
    fun page(
        @ApiParam("资源类型", required = false)
        @RequestParam type: ResourceType?,
        @ApiParam("项目名", required = false)
        @RequestParam projectId: String?,
        @ApiParam("仓库名", required = false)
        @RequestParam repoName: String?,
        @ApiParam("操作人", required = false)
        @RequestParam operator: String?,
        @ApiParam("开始时间", required = false)
        @RequestParam startTime: String?,
        @ApiParam("结束时间", required = false)
        @RequestParam endTime: String?,
        @ApiParam("页数", required = false, defaultValue = "1")
        @RequestParam pageNumber: Int?,
        @ApiParam("每页数量", required = false, defaultValue = "20")
        @RequestParam pageSize: Int?
    ): Response<Page<OperateLogResponse>> {
        val page = operateLogService.page(
            type, projectId, repoName,
            operator, startTime, endTime, pageNumber ?: 1, pageSize ?: 20
        )
        return ResponseBuilder.success(page)
    }
}

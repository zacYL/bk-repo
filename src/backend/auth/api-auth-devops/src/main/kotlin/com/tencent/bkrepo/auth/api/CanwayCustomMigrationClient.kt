package com.tencent.bkrepo.auth.api

import com.tencent.bkrepo.auth.pojo.migration.ActionDeleteDTO
import com.tencent.bkrepo.common.api.constant.AUTH_HEADER_DEVOPS_UID
import com.tencent.bkrepo.common.api.constant.DEVOPS_AUTH_SERVICE_NAME
import com.tencent.bkrepo.common.api.pojo.Response
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping

@Api("自定义迁移")
@Primary
@FeignClient(DEVOPS_AUTH_SERVICE_NAME, contextId = "CanwayCustomMigrationClient")
@RequestMapping("/api/service/custom/migration")
interface CanwayCustomMigrationClient {

    @ApiOperation("删除指定动作")
    @PostMapping("/action/delete")
    fun deleteAction(
        @RequestHeader(AUTH_HEADER_DEVOPS_UID)
        @ApiParam(value = "用户ID", required = true)
        userId: String,
        @ApiParam(value = "动作删除", required = true)
        @RequestBody
        request: ActionDeleteDTO,
    ): Response<Boolean>
}

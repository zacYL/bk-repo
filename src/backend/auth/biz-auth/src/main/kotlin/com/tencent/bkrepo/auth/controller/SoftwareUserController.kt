package com.tencent.bkrepo.auth.controller

import com.tencent.bkrepo.auth.pojo.software.UnitType
import com.tencent.bkrepo.auth.pojo.software.request.UseUnitDeleteRequest
import com.tencent.bkrepo.auth.pojo.software.response.SoftwareUseUnitResponse
import com.tencent.bkrepo.auth.service.software.SoftwareUserService
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.web.bind.annotation.*


@Api("软件源仓库权限用户列表, ")
@RestController
@RequestMapping("/api/bksoftware/unit")
class SoftwareUserController(
    private val softwareUserService: SoftwareUserService
) {
    @ApiOperation("加载仓库下用户/部门")
    @GetMapping("/{repoName}")
    fun user(
        @ApiParam("仓库名", required = true)
        @PathVariable repoName: String
    ): Response<SoftwareUseUnitResponse> {
        val response = softwareUserService.unit(repoName)
        return ResponseBuilder.success(response)

    }

    @ApiOperation("修改用户/部门权限")
    @PutMapping("/permission/update/{repoName}")
    fun updatePermission(
        @ApiParam("仓库名", required = true)
        @PathVariable repoName: String,
        @ApiParam("用户/部门 id 列表", required = true)
        @RequestBody set: Set<String>,
        @ApiParam("数据类型", required = true, example = "USER")
        @RequestParam unitType: UnitType,
        @ApiParam("是否赋予推送权限", required = true, defaultValue = "false")
        @RequestParam push: Boolean = false
    ): Response<Boolean> {
        softwareUserService.updatePermission(repoName, set, unitType, push)
        return ResponseBuilder.success()
    }

    @ApiOperation("添加用户/部门")
    @PostMapping("/{repoName}")
    fun addUnit(
        @ApiParam("仓库名", required = true)
        @PathVariable repoName: String,
        @ApiParam("用户/部门 id 列表", required = true)
        @RequestBody set: Set<String>,
        @ApiParam("数据类型", required = true, example = "USER")
        @RequestParam unitType: UnitType,
        @ApiParam("是否赋予推送权限", required = false, defaultValue = "false")
        @RequestParam push: Boolean
    ): Response<Boolean> {
        softwareUserService.addUnit(repoName, set, unitType, push)
        return ResponseBuilder.success()
    }

    @ApiOperation("删除用户/部门")
    @DeleteMapping("/{repoName}")
    fun deleteUnit(
        @ApiParam("仓库名", required = true)
        @PathVariable repoName: String,
        @ApiParam("用户/部门 id 列表", required = true)
        @RequestBody useUnitDeleteRequest: UseUnitDeleteRequest
    ): Response<Boolean> {
        softwareUserService.deleteUnit(repoName, useUnitDeleteRequest)
        return ResponseBuilder.success()
    }

}

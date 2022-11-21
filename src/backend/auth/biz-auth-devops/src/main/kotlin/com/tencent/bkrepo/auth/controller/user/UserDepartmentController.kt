/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.auth.controller.user

import com.tencent.bkrepo.auth.constant.AUTH_API_DEPARTMENT_PREFIX
import com.tencent.bkrepo.auth.service.DepartmentService
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.devops.pojo.BkChildrenDepartment
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(AUTH_API_DEPARTMENT_PREFIX)
class UserDepartmentController {

    @Autowired(required = false)
    lateinit var departmentService: DepartmentService

    @ApiOperation("查询该部门下一级部门列表")
    @GetMapping("/list")
    fun listDepartment(
        @RequestAttribute("userId") userId: String,
        @ApiParam("部门ID, 不传默认返回所有根部门")
        @RequestParam departmentId: Int?
    ): Response<List<BkChildrenDepartment>?> {
        return ResponseBuilder.success(departmentService.listDepartmentById(userId, departmentId))
    }

    @ApiOperation("查询项目下有权限的部门列表: CI 项目下有权限的部门")
    @GetMapping("/list/{projectId}")
    fun listDepartmentByProjectId(
        @RequestAttribute("userId") userId: String,
        @ApiParam("项目ID", required = true)
        @PathVariable projectId: String
    ): Response<List<String>> {
        return ResponseBuilder.success(departmentService.listDepartmentByProjectId(userId, projectId))
    }

    @ApiOperation("批量查询部门名称")
    @PostMapping("/listByIds")
    fun listDepartmentByIds(
        @RequestAttribute("userId") userId: String,
        @RequestBody departmentIds: List<Int>
    ): Response<List<BkChildrenDepartment>?> {
        return ResponseBuilder.success(departmentService.listDepartmentByIds(userId, departmentIds))
    }
}

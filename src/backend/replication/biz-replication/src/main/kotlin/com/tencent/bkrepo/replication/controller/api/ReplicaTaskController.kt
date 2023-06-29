/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.bkrepo.replication.controller.api

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.replication.pojo.task.ReplicaTaskDetail
import com.tencent.bkrepo.replication.pojo.task.ReplicaTaskInfo
import com.tencent.bkrepo.replication.pojo.task.request.ReplicaTaskCopyRequest
import com.tencent.bkrepo.replication.pojo.task.request.ReplicaTaskCreateRequest
import com.tencent.bkrepo.replication.pojo.task.request.ReplicaTaskUpdateRequest
import com.tencent.bkrepo.replication.pojo.task.request.TaskPageParam
import com.tencent.bkrepo.replication.service.ReplicaTaskService
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.web.bind.annotation.*

/**
 * 数据同步任务接口
 */
@Api("任务接口")
@RestController
@RequestMapping("/api/task")
class ReplicaTaskController(
    private val replicaTaskService: ReplicaTaskService
) {
    @ApiOperation("创建任务")
    @PostMapping("/{projectId}/create")
    @Permission(ResourceType.REPLICATION, PermissionAction.CREATE)
    fun create(
        @PathVariable projectId: String,
        @RequestBody request: ReplicaTaskCreateRequest
    ): Response<ReplicaTaskInfo> {
        return ResponseBuilder.success(replicaTaskService.create(request))
    }

    @ApiOperation("根据任务key查询任务信息")
    @GetMapping("/info/{projectId}/{key}")
    @Permission(ResourceType.REPLICATION, PermissionAction.VIEW)
    fun getByTaskKey(
        @PathVariable projectId: String,
        @PathVariable key: String
    ): Response<ReplicaTaskInfo> {
        return ResponseBuilder.success(replicaTaskService.getByTaskKey(key))
    }

    @ApiOperation("根据任务key查询任务详情")
    @GetMapping("/detail/{projectId}/{key}")
    @Permission(ResourceType.REPLICATION, PermissionAction.VIEW)
    fun getDetailByTaskKey(
        @PathVariable projectId: String,
        @PathVariable key: String
    ): Response<ReplicaTaskDetail> {
        return ResponseBuilder.success(replicaTaskService.getDetailByTaskKey(key))
    }

    @ApiOperation("分页查询任务")
    @GetMapping("/page/{projectId}")
    @Permission(ResourceType.REPLICATION, PermissionAction.VIEW)
    fun listReplicationTaskInfoPage(
        @PathVariable projectId: String,
        option: TaskPageParam
    ): Response<Page<ReplicaTaskInfo>> {
        return ResponseBuilder.success(replicaTaskService.listTasksPage(projectId, option))
    }

    @ApiOperation("删除任务")
    @DeleteMapping("/delete/{projectId}/{key}")
    @Permission(ResourceType.REPLICATION, PermissionAction.DELETE)
    fun delete(
        @PathVariable projectId: String,
        @PathVariable key: String
    ): Response<Void> {
        replicaTaskService.deleteByTaskKey(key)
        return ResponseBuilder.success()
    }

    @ApiOperation("任务启停状态切换")
    @PostMapping("/toggle/status/{projectId}/{key}")
    @Permission(ResourceType.REPLICATION, PermissionAction.ENABLE)
    fun toggleStatus(
        @PathVariable projectId: String,
        @PathVariable key: String
    ): Response<Void> {
        replicaTaskService.toggleStatus(key)
        return ResponseBuilder.success()
    }

    @ApiOperation("任务复制")
    @PostMapping("/{projectId}/copy")
    @Permission(ResourceType.REPLICATION, PermissionAction.COPY)
    fun copy(
        @PathVariable projectId: String,
        @RequestBody request: ReplicaTaskCopyRequest
    ): Response<Void> {
        replicaTaskService.copy(request)
        return ResponseBuilder.success()
    }

    @ApiOperation("任务更新")
    @PostMapping("/{projectId}/update")
    @Permission(ResourceType.REPLICATION, PermissionAction.UPDATE)
    fun update(
        @PathVariable projectId: String,
        @RequestBody request: ReplicaTaskUpdateRequest
    ): Response<Void> {
        replicaTaskService.update(request)
        return ResponseBuilder.success()
    }

    @ApiOperation("执行计划")
    @PostMapping("/execute/{projectId}/{key}")
    @Permission(ResourceType.REPLICATION, PermissionAction.EXECUTE)
    fun execute(
        @PathVariable projectId: String,
        @PathVariable key: String
    ): Response<Void> {
        replicaTaskService.execute(key)
        return ResponseBuilder.success()
    }
}

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
import com.tencent.bkrepo.replication.pojo.record.RecordOverview
import com.tencent.bkrepo.replication.pojo.record.ReplicaOverview
import com.tencent.bkrepo.replication.pojo.record.ReplicaRecordDetail
import com.tencent.bkrepo.replication.pojo.record.ReplicaRecordDetailListOption
import com.tencent.bkrepo.replication.pojo.record.ReplicaRecordInfo
import com.tencent.bkrepo.replication.pojo.record.ReplicaRecordListOption
import com.tencent.bkrepo.replication.pojo.record.ReplicaTaskRecordInfo
import com.tencent.bkrepo.replication.service.ReplicaRecordService
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 任务执行日志接口
 */
@Api("任务执行日志接口")
@RestController
@RequestMapping("/api/task/record")
class ReplicaRecordController(
    private val replicaRecordService: ReplicaRecordService
) {
    @ApiOperation("根据recordId查询任务执行日志详情")
    @GetMapping("/{projectId}/{recordId}")
    @Permission(ResourceType.REPLICATION, PermissionAction.VIEW)
    fun getRecordAndTaskInfoByRecordId(
        @PathVariable projectId: String,
        @PathVariable recordId: String
    ): Response<ReplicaTaskRecordInfo> {
        return ResponseBuilder.success(replicaRecordService.getRecordAndTaskInfoByRecordId(recordId))
    }

    @ApiOperation("根据taskKey查询任务执行日志")
    @GetMapping("/list/{projectId}/{key}")
    @Permission(ResourceType.REPLICATION, PermissionAction.VIEW)
    fun listRecordsByTaskKey(
        @PathVariable projectId: String,
        @PathVariable key: String
    ): Response<List<ReplicaRecordInfo>> {
        return ResponseBuilder.success(replicaRecordService.listRecordsByTaskKey(key))
    }

    @ApiOperation("根据taskKey分页查询任务执行日志")
    @GetMapping("/page/{projectId}/{key}")
    @Permission(ResourceType.REPLICATION, PermissionAction.VIEW)
    fun listRecordsPage(
        @PathVariable projectId: String,
        @PathVariable key: String,
        option: ReplicaRecordListOption
    ): Response<Page<ReplicaRecordInfo>> {
        return ResponseBuilder.success(replicaRecordService.listRecordsPage(key, option))
    }

    @ApiOperation("根据recordId查询任务执行日志详情")
    @GetMapping("/detail/list/{projectId}/{recordId}")
    @Permission(ResourceType.REPLICATION, PermissionAction.VIEW)
    fun listDetailsByRecordId(
        @PathVariable projectId: String,
        @PathVariable recordId: String
    ): Response<List<ReplicaRecordDetail>> {
        return ResponseBuilder.success(replicaRecordService.listDetailsByRecordId(recordId))
    }

    @ApiOperation("根据recordId分页查询任务执行日志详情")
    @GetMapping("/detail/page/{projectId}/{recordId}")
    @Permission(ResourceType.REPLICATION, PermissionAction.VIEW)
    fun listRecordDetailPage(
        @PathVariable projectId: String,
        @ApiParam(value = "执行记录id", required = true)
        @PathVariable recordId: String,
        option: ReplicaRecordDetailListOption
    ): Response<Page<ReplicaRecordDetail>> {
        return ResponseBuilder.success(replicaRecordService.listRecordDetailPage(recordId, option))
    }

    @ApiOperation("根据recordId查询任务执行日志详情总览")
    @GetMapping("/detail/overview/{projectId}/{recordId}")
    @Permission(ResourceType.REPLICATION, PermissionAction.VIEW)
    fun recordDetailOverview(
        @PathVariable projectId: String,
        @ApiParam(value = "执行记录id", required = true)
        @PathVariable recordId: String,
    ): Response<RecordOverview> {
        return ResponseBuilder.success(replicaRecordService.recordDetailOverview(recordId))
    }

    @ApiOperation("根据recordId查询任务执行总览信息")
    @GetMapping("/overview/{recordId}")
    @Permission(ResourceType.REPLICATION, PermissionAction.VIEW)
    fun getRecordOverviewByRecordId(
        @PathVariable projectId: String,
        @PathVariable recordId: String
    ): Response<ReplicaOverview?> {
        return ResponseBuilder.success(replicaRecordService.getRecordById(recordId)?.replicaOverview)
    }
}

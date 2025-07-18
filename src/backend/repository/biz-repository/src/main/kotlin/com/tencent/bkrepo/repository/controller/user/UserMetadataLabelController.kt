/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.bkrepo.repository.controller.user

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.metadata.permission.PermissionManager
import com.tencent.bkrepo.common.metadata.service.metadata.MetadataLabelService
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.pojo.metadata.label.MetadataLabelDetail
import com.tencent.bkrepo.repository.pojo.metadata.label.MetadataLabelRequest
import com.tencent.bkrepo.repository.pojo.metadata.label.UserLabelCreateRequest
import com.tencent.bkrepo.repository.pojo.metadata.label.UserLabelUpdateRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "元数据标签管理接口")
@RestController
@RequestMapping("/api/metadata/label")
class UserMetadataLabelController(
    private val metadataLabelService: MetadataLabelService,
    private val permissionManager: PermissionManager
) {

    @Operation(summary = "创建标签")
    @PostMapping("/{projectId}")
    fun create(
        @PathVariable projectId: String,
        @RequestBody userLabelCreateRequest: UserLabelCreateRequest
    ): Response<Void> {
        permissionManager.checkProjectPermission(PermissionAction.MANAGE, projectId)
        val request = MetadataLabelRequest(
            projectId = projectId,
            labelKey = userLabelCreateRequest.labelKey,
            labelColorMap = userLabelCreateRequest.labelColorMap,
            display = userLabelCreateRequest.display,
            enumType = userLabelCreateRequest.enumType,
            enableColorConfig = userLabelCreateRequest.enableColorConfig,
            category = userLabelCreateRequest.category,
            description = userLabelCreateRequest.description,
        )
        metadataLabelService.create(request)
        return ResponseBuilder.success()
    }

    @Operation(summary = "更新标签")
    @PutMapping("/{projectId}/{labelKey}")
    fun update(
        @PathVariable projectId: String,
        @PathVariable labelKey: String,
        @RequestBody userLabelUpdateRequest: UserLabelUpdateRequest
    ): Response<Void> {
        permissionManager.checkProjectPermission(PermissionAction.MANAGE, projectId)
        val request = MetadataLabelRequest(
            projectId = projectId,
            labelKey = labelKey,
            labelColorMap = userLabelUpdateRequest.labelColorMap,
            display = userLabelUpdateRequest.display,
            enumType = userLabelUpdateRequest.enumType,
            enableColorConfig = userLabelUpdateRequest.enableColorConfig,
            category = userLabelUpdateRequest.category,
            description = userLabelUpdateRequest.description,
        )
        metadataLabelService.update(request)
        return ResponseBuilder.success()
    }

    @Operation(summary = "批量保存")
    @PostMapping("/batch/{projectId}")
    fun batchSave(
        @PathVariable projectId: String,
        @RequestBody userRequests: List<UserLabelCreateRequest>
    ): Response<Void> {
        permissionManager.checkProjectPermission(PermissionAction.MANAGE, projectId)
        val requests = userRequests.map {
            MetadataLabelRequest(
                projectId = projectId,
                labelKey = it.labelKey,
                labelColorMap = it.labelColorMap,
                display = it.display,
                enumType = it.enumType,
                enableColorConfig = it.enableColorConfig,
                category = it.category,
                description = it.description,
            )
        }
        metadataLabelService.batchSave(requests)
        return ResponseBuilder.success()
    }

    @Operation(summary = "查询项目下所有标签")
    @GetMapping("/{projectId}")
    fun list(
        @PathVariable projectId: String
    ): Response<List<MetadataLabelDetail>> {
        permissionManager.checkProjectPermission(PermissionAction.READ, projectId)
        return ResponseBuilder.success(metadataLabelService.listAll(projectId))
    }

    @Operation(summary = "查询标签详情")
    @GetMapping("/{projectId}/{labelKey}")
    fun detail(
        @PathVariable projectId: String,
        @PathVariable labelKey: String,
    ): Response<MetadataLabelDetail> {
        permissionManager.checkProjectPermission(PermissionAction.READ, projectId)
        return ResponseBuilder.success(metadataLabelService.detail(projectId, labelKey))
    }

    @Operation(summary = "删除标签")
    @DeleteMapping("/{projectId}/{labelKey}")
    fun delete(
        @PathVariable projectId: String,
        @PathVariable labelKey: String
    ): Response<Void> {
        permissionManager.checkProjectPermission(PermissionAction.MANAGE, projectId)
        metadataLabelService.delete(projectId, labelKey)
        return ResponseBuilder.success()
    }
}

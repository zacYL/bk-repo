/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 Tencent.  All rights reserved.
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

package com.tencent.bkrepo.auth.controller.service

import com.tencent.bkrepo.auth.api.ServicePermissionClient
import com.tencent.bkrepo.auth.controller.OpenResource
import com.tencent.bkrepo.auth.pojo.permission.CheckPermissionRequest
import com.tencent.bkrepo.auth.pojo.permission.ListPathResult
import com.tencent.bkrepo.auth.service.PermissionService
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.query.enums.OperationType
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController

@RestController
class ServicePermissionController @Autowired constructor(
    private val permissionService: PermissionService
) : ServicePermissionClient, OpenResource(permissionService) {


    /**
     * 本接口不做权限校验，status表明是否需要做校验
     * OperationType IN  表示有权限的路径列表，需要做交集
     * OperationType NIN 表示有无权限的路径列表，需要做差集
     */
    override fun listPermissionPath(userId: String, projectId: String, repoName: String): Response<ListPathResult> {
        val repoAccessControl = permissionService.checkRepoAccessControl(projectId, repoName)
        if (repoAccessControl) {
            val permissionPath = permissionService.listPermissionPath(userId, projectId, repoName)
            if (permissionPath == null) {
                val result = ListPathResult(status = false, path = mapOf(OperationType.IN to emptyList()))
                return ResponseBuilder.success(result)
            }
            val result = ListPathResult(status = true, path = mapOf(OperationType.IN to permissionPath))
            return ResponseBuilder.success(result)
        } else {
            val permissionPath = permissionService.listNoPermissionPath(userId, projectId, repoName)
            val status = permissionPath.isNotEmpty()
            val result = ListPathResult(status = status, path = mapOf(OperationType.NIN to permissionPath))
            return ResponseBuilder.success(result)
        }
    }


    override fun checkPermission(request: CheckPermissionRequest): Response<Boolean> {
        checkRequest(request)
        return ResponseBuilder.success(permissionService.checkPermission(request))
    }

    override fun listPermissionRepo(projectId: String, userId: String, appId: String?): Response<List<String>> {
        return ResponseBuilder.success(permissionService.listPermissionRepo(projectId, userId, appId))
    }

    override fun listPermissionProject(userId: String): Response<List<String>> {
        return ResponseBuilder.success(permissionService.listPermissionProject(userId))
    }

}

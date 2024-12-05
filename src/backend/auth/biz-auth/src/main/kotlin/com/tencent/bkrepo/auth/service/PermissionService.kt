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

package com.tencent.bkrepo.auth.service

import com.tencent.bkrepo.auth.pojo.RegisterResourceRequest
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.auth.pojo.permission.CheckPermissionRequest
import com.tencent.bkrepo.auth.pojo.permission.CreatePermissionRequest
import com.tencent.bkrepo.auth.pojo.permission.Permission
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionActionRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionDepartmentRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionPathRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionRepoRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionRoleRequest
import com.tencent.bkrepo.auth.pojo.permission.UpdatePermissionUserRequest

interface PermissionService {

    fun checkPermission(request: CheckPermissionRequest): Boolean

    fun listPermissionRepo(
        projectId: String,
        userId: String,
        appId: String?,
        actions: List<PermissionAction>?,
        includePathAuthRepo: Boolean
    ): List<String>

    fun listPermissionProject(userId: String): List<String>

    fun createPermission(request: CreatePermissionRequest): Boolean

    fun listPermission(projectId: String, repoName: String?): List<Permission>

    fun listBuiltinPermission(projectId: String, repoName: String): List<Permission>

    fun deletePermission(id: String): Boolean

    fun updateIncludePath(request: UpdatePermissionPathRequest): Boolean

    fun updateExcludePath(request: UpdatePermissionPathRequest): Boolean

    fun updateRepoPermission(request: UpdatePermissionRepoRequest): Boolean

    fun updatePermissionUser(request: UpdatePermissionUserRequest): Boolean

    fun updatePermissionRole(request: UpdatePermissionRoleRequest): Boolean

    fun updatePermissionDepartment(request: UpdatePermissionDepartmentRequest): Boolean

    fun updatePermissionAction(request: UpdatePermissionActionRequest): Boolean

    fun registerResource(request: RegisterResourceRequest)

    fun listProjectBuiltinPermission(projectId: String): List<Permission>

    fun isProjectManager(userId: String): Boolean

    fun findPermissionById(id: String): Permission?

    fun findOneByPermNameAndProjectIdAndResourceTypeAndRepos(
        permName: String,
        projectId: String?,
        resourceType: ResourceType,
        repo: String
    ): Permission?

    fun updatePermissionById(id: String, key: String, value: Any): Boolean

    fun updatePermissionById(id: String, request: UpdatePermissionRequest): Boolean

    fun listBuiltinPermissionNoBack(projectId: String, repoName: String)

    /**
     * 查询用户是否为管理员
     * @param projectId 项目ID
     * @param tenantId 租户ID
     */
    fun isAdmin(userId: String, projectId: String?, tenantId: String?): Boolean

    fun deletePermissionData(projectId: String, repoName: String): Boolean

    fun listNodePermission(projectId: String, repoName: String?): List<Permission>

    fun getUserAuthPaths(
        userId: String,
        projectId: String,
        repoNames: List<String>,
        action: PermissionAction
    ): Map<String, List<String>>
}

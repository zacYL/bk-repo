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

package com.tencent.bkrepo.opdata.controller

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.metadata.permission.PermissionManager
import com.tencent.bkrepo.common.security.permission.PrincipalType
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.fs.server.api.FsClientClient
import com.tencent.bkrepo.fs.server.pojo.ClientDetail
import com.tencent.bkrepo.fs.server.pojo.ClientListRequest
import com.tencent.bkrepo.fs.server.pojo.DailyClientDetail
import com.tencent.bkrepo.fs.server.pojo.DailyClientListRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/fs-client")
class FsClientController(
    private val fsClientClient: FsClientClient,
    private val permissionManager: PermissionManager
) {

    @GetMapping("/list")
    fun getClients(request: ClientListRequest): Response<Page<ClientDetail>> {
        if (request.projectId.isNullOrBlank()) {
            permissionManager.checkPrincipal(SecurityUtils.getUserId(), PrincipalType.ADMIN)
        } else {
            permissionManager.checkProjectPermission(PermissionAction.READ, request.projectId!!)
        }
        return fsClientClient.listClients(
            request.projectId,
            request.repoName,
            request.pageNumber,
            request.pageSize,
            request.online,
            request.ip,
            request.userId,
            request.version
        )
    }

    @GetMapping("/daily/list")
    fun getDailyClients(request: DailyClientListRequest): Response<Page<DailyClientDetail>> {
        if (request.projectId.isNullOrBlank()) {
            permissionManager.checkPrincipal(SecurityUtils.getUserId(), PrincipalType.ADMIN)
        } else {
            permissionManager.checkProjectPermission(PermissionAction.READ, request.projectId!!)
        }
        var action = ""
        if (!request.actions.isEmpty()) {
            request.actions.forEach { action = action + "," + it }
            action = action.substring(1)
        }
        return fsClientClient.listDailyClients(
            request.projectId,
            request.repoName,
            request.pageNumber,
            request.pageSize,
            request.ip,
            request.version,
            request.startTime,
            request.endTime,
            request.mountPoint,
            action
        )
    }

}

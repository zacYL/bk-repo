/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2024 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.go.controller

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.constant.MediaTypes
import com.tencent.bkrepo.common.api.exception.MethodNotAllowedException
import com.tencent.bkrepo.common.artifact.api.DefaultArtifactInfo
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Suppress("MVCPathVariableInspection")
@RequestMapping("/{projectId}/{repoName}/sumdb/{databaseURL}")
@RestController
class GoChecksumController {

    @GetMapping("/supported")
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    fun getSupported(artifactInfo: DefaultArtifactInfo) {
//        HttpContextHolder.getResponse().status = HttpStatus.OK.value
        HttpContextHolder.getResponse().status = HttpStatus.NOT_FOUND.value
    }

    @GetMapping("/latest", produces = [MediaTypes.TEXT_PLAIN])
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    fun getLatestSignedTree(artifactInfo: DefaultArtifactInfo): String {
        throw MethodNotAllowedException()
    }

    @GetMapping("/lookup/**@{version}", produces = [MediaTypes.TEXT_PLAIN])
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    fun lookup(artifactInfo: DefaultArtifactInfo) {
        throw MethodNotAllowedException()
    }

    @GetMapping("/tile/{height}/{level}/{k}[.p/{w}]")
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    fun getLogTile(artifactInfo: DefaultArtifactInfo) {
        throw MethodNotAllowedException()
    }

    @GetMapping("/tile/{height}/data/{k}[.p/{w}]")
    @Permission(type = ResourceType.REPO, action = PermissionAction.READ)
    fun getLeafData(artifactInfo: DefaultArtifactInfo) {
        throw MethodNotAllowedException()
    }
}

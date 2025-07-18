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

package com.tencent.bkrepo.helm.api

import com.tencent.bkrepo.common.api.constant.HELM_SERVICE_NAME
import com.tencent.bkrepo.common.api.pojo.Response
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

/**
 * helm代理仓库刷新接口
 */
@Tag(name = "helm代理仓库刷新接口")
@Primary
@FeignClient(HELM_SERVICE_NAME, contextId = "HelmClient")
@RequestMapping("/service/index")
interface HelmClient {

    @Operation(summary = "刷新对应代理仓库的index文件以及package信息")
    @PostMapping("/{projectId}/{repoName}/refresh")
    fun refreshIndexYamlAndPackage(
        @PathVariable projectId: String,
        @PathVariable repoName: String
    ): Response<Void>

    @Operation(summary = "初始化代理仓库的index文件以及package信息")
    @PostMapping("/{projectId}/{repoName}/init")
    fun initIndexAndPackage(
        @PathVariable projectId: String,
        @PathVariable repoName: String
    ): Response<Void>

    @Operation(summary = "当仓库有版本replication时，刷新index文件")
    @PostMapping("/{projectId}/{repoName}/replication")
    fun refreshIndexForReplication(
        @PathVariable projectId: String,
        @PathVariable repoName: String,
        @RequestParam packageName: String,
        @RequestParam packageKey: String,
        @RequestParam packageVersion: String,
    ): Response<Void>

    @Operation(summary = "删除仓库下的包版本")
    @DeleteMapping("version/delete/{projectId}/{repoName}")
    fun deleteVersion(
        @PathVariable projectId: String,
        @PathVariable repoName: String,
        @RequestParam packageName: String,
        @RequestParam version: String
    ): Response<Void>

    @Operation(summary = "更新对应节点元数据")
    @PostMapping("/{projectId}/{repoName}/metaDate/regenerate")
    fun metadataRefresh(
        @PathVariable projectId: String,
        @PathVariable repoName: String
    ): Response<Void>

}

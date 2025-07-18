/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 Tencent.  All rights reserved.
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

package com.tencent.bkrepo.analyst.service

import com.tencent.bkrepo.analyst.pojo.CheckForbidResult
import com.tencent.bkrepo.analyst.pojo.request.ScanQualityUpdateRequest
import com.tencent.bkrepo.analyst.pojo.response.ScanQuality
import com.tencent.bkrepo.common.analysis.pojo.scanner.Scanner

interface ScanQualityService {
    /**
     * 获取方案质量规则
     */
    fun getScanQuality(planId: String): ScanQuality

    /**
     * 更新方案质量规则
     */
    fun updateScanQuality(planId: String, request: ScanQualityUpdateRequest): Boolean

    /**
     * 检查是否通过质量规则
     */
    fun checkScanQualityRedLine(planId: String, scanResultOverview: Map<String, Number>): Boolean

    /**
     * 检查是否通过质量规则
     */
    fun checkScanQualityRedLine(
        scanQuality: Map<String, Any>,
        scanResultOverview: Map<String, Number>,
        scanner: Scanner
    ): Boolean

    /**
     * 是否要禁用
     *
     * @param projectId 项目ID
     * @param repoName 仓库名
     * @param repoType 仓库类型
     * @param fullPath 制品路径
     * @param sha256 制品sha256
     *
     * @return 是否禁用制品检查结果
     */
    fun shouldForbid(
        projectId: String,
        repoName: String,
        repoType: String,
        fullPath: String,
        sha256: String
    ): CheckForbidResult

    /**
     * 是否禁用未扫描制品
     *
     * @param projectId 项目ID
     * @param repoName 仓库名
     * @param repoType 仓库类型
     * @param fullPath 制品路径
     *
     * @return 是否禁用制品
     */
    fun shouldForbidBeforeScanned(projectId: String, repoName: String, repoType: String, fullPath: String): Boolean

    /**
     * 是否禁用未扫描制品
     *
     * @param projectId 项目ID
     * @param repoName 仓库名
     * @param repoType 仓库类型
     * @param packageName 包名
     * @param packageVersion 包版本
     *
     * @return 是否禁用制品
     */
    fun shouldForbidBeforeScanned(
        projectId: String,
        repoName: String,
        repoType: String,
        packageName: String,
        packageVersion: String
    ): Boolean
}

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

package com.tencent.bkrepo.analyst.pojo.license

import io.swagger.v3.oas.annotations.media.Schema


@Schema(title = "SPDX许可证信息")
data class SpdxLicenseInfo(
    @get:Schema(title = "创建者")
    val createdBy: String,
    @get:Schema(title = "创建时间")
    val createdDate: String,
    @get:Schema(title = "修改者")
    val lastModifiedBy: String,
    @get:Schema(title = "修改时间")
    val lastModifiedDate: String,
    @get:Schema(title = "许可证名称")
    val name: String,
    @get:Schema(title = "许可证标识符")
    val licenseId: String,
    @get:Schema(title = "指向其他许可证副本的交叉引用 URL")
    val seeAlso: MutableList<String>,
    @get:Schema(title = "对许可证文件的 HTML 格式的引用")
    val reference: String,
    @get:Schema(title = "是否被弃用")
    val isDeprecatedLicenseId: Boolean,
    @get:Schema(title = "OSI 是否已批准许可证")
    val isOsiApproved: Boolean,
    @get:Schema(title = "是否 FSF 认证免费")
    val isFsfLibre: Boolean?,
    @get:Schema(title = "包含许可证详细信息的 JSON 文件的 URL")
    val detailsUrl: String,
    @get:Schema(title = "是否信任")
    val isTrust: Boolean,
    @get:Schema(title = "风险等级")
    val risk: String?
)

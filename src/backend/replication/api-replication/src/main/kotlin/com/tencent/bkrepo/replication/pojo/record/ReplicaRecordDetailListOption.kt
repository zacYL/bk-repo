/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

package com.tencent.bkrepo.replication.pojo.record

import com.tencent.bkrepo.common.api.constant.DEFAULT_PAGE_NUMBER
import com.tencent.bkrepo.common.api.constant.DEFAULT_PAGE_SIZE
import io.swagger.v3.oas.annotations.media.Schema


@Schema(title = "执行日志详情列表选项")
class ReplicaRecordDetailListOption(
    @get:Schema(title = "当前页")
    val pageNumber: Int = DEFAULT_PAGE_NUMBER,
    @get:Schema(title = "分页大小")
    val pageSize: Int = DEFAULT_PAGE_SIZE,
    @get:Schema(title = "包名称, 根据该字段模糊搜索")
    val packageName: String? = null,
    @get:Schema(title = "仓库名称")
    val repoName: String? = null,
    @get:Schema(title = "远程节点名称")
    val clusterName: String? = null,
    @get:Schema(title = "路径名称, 根据该字段前缀匹配")
    val path: String? = null,
    @get:Schema(title = "执行状态")
    val status: ExecutionStatus? = null,
    @get:Schema(title = "制品名称")
    var artifactName: String? = null,
    @get:Schema(title = "版本")
    var version: String? = null
)

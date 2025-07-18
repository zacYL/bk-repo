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

package com.tencent.bkrepo.analyst.pojo

/**
 * 文件节点
 */
data class Node(
    /**
     * 文件所属项目
     */
    val projectId: String,
    /**
     * 文件所属仓库
     */
    val repoName: String,
    /**
     * 文件完整路径
     */
    val fullPath: String,
    /**
     * 制品名
     */
    var artifactName: String,
    /**
     * 依赖包唯一标识，制品为依赖包时候存在
     */
    var packageKey: String? = null,
    /**
     * 依赖包版本
     */
    var packageVersion: String? = null,
    /**
     * 文件sha256
     */
    val sha256: String,
    /**
     * 文件大小
     */
    var size: Long,
    /**
     * package大小
     */
    var packageSize: Long,
    /**
     * 最后修改人
     */
    val lastModifiedBy: String,
)

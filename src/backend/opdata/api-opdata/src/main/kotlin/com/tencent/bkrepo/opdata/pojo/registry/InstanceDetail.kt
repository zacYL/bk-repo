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

package com.tencent.bkrepo.opdata.pojo.registry

import io.swagger.v3.oas.annotations.media.Schema


/**
 * 服务节点详细信息
 */
data class InstanceDetail(
    @get:Schema(title = "正在下载的请求数量", required = true)
    val downloadingCount:Long,
    @get:Schema(title = "正在上传的请求数量", required = true)
    val uploadingCount: Long,
    @get:Schema(title = "从缓存异步上传到实际存储的任务数量，不使用缓存时为-1", required = true)
    val asyncTaskActiveCount: Long,
    @get:Schema(title = "从缓存异步上传到实际存储的任务队列大小，不使用缓存时为-1", required = true)
    val asyncTaskQueueSize: Long,
    @get:Schema(title = "已加载的插件", required = true)
    val loadedPlugins: List<String>? = null
)

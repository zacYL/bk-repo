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

package com.tencent.bkrepo.common.storage.config

import org.springframework.util.unit.DataSize
import java.time.Duration

data class MonitorProperties(
    var enabled: Boolean = false,
    var fallbackLocation: String? = null,
    var enableTransfer: Boolean = false,
    /**
     * healthy状态时健康检查间隔
     */
    var interval: Duration = Duration.ofSeconds(10),
    /**
     * unhealthy状态时健康检查间隔，设置较短时间可提高检查频率，尽早恢复为healthy状态，但是会频繁读写存储
     */
    var failedInterval: Duration = Duration.ofSeconds(5),
    var dataSize: DataSize = DataSize.ofMegabytes(1),
    var timeout: Duration = Duration.ofSeconds(5),
    var timesToRestore: Int = 5,
    var timesToFallback: Int = 3
)

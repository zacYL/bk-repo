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

package com.tencent.bkrepo.generic.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty

@ConfigurationProperties(prefix = "generic")
data class GenericProperties(
    /**
     * generic服务domain地址，用于生成临时url
     */
    var domain: String = "localhost",
    @NestedConfigurationProperty
    var delta: DeltaProperties = DeltaProperties(),
    @NestedConfigurationProperty
    var bkBase: BkBaseProperties = BkBaseProperties(),
    /**
     * 平台账号，在Generic仓库代理远程BkRepo的Generic仓库时使用
     */
    var platforms: List<PlatformProperties> = emptyList(),
    /**
     * 分块上传来源判断
     */
    var chunkedUploadClients: List<String> = listOf("bk-repo"),
    @NestedConfigurationProperty
    var proxy: ProxyProperties = ProxyProperties(),
    @NestedConfigurationProperty
    var userShareInterceptor: UserShareInterceptorProperties = UserShareInterceptorProperties(),
    @NestedConfigurationProperty
    var compressedReport: CompressedReportProperties = CompressedReportProperties(),
)

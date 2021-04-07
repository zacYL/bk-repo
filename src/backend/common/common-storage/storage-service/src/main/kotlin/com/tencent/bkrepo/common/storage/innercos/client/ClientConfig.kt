/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.common.storage.innercos.client

import com.tencent.bkrepo.common.storage.credentials.InnerCosCredentials
import com.tencent.bkrepo.common.storage.innercos.cl5.CL5Info
import com.tencent.bkrepo.common.storage.innercos.endpoint.CL5EndpointResolver
import com.tencent.bkrepo.common.storage.innercos.endpoint.DefaultEndpointResolver
import com.tencent.bkrepo.common.storage.innercos.endpoint.EndpointResolver
import com.tencent.bkrepo.common.storage.innercos.endpoint.RegionEndpointBuilder
import com.tencent.bkrepo.common.storage.innercos.http.HttpProtocol
import org.springframework.util.unit.DataSize
import java.time.Duration

class ClientConfig(private val credentials: InnerCosCredentials) {
    val maxUploadParts: Int = MAX_PARTS
    val signExpired: Duration = Duration.ofDays(1)
    val httpProtocol: HttpProtocol = HttpProtocol.HTTP

    val multipartUploadThreshold: Long = DataSize.ofMegabytes(MULTIPART_THRESHOLD_SIZE).toBytes()
    val minimumUploadPartSize: Long = DataSize.ofMegabytes(MIN_PART_SIZE).toBytes()

    val endpointResolver = createEndpointResolver()
    val endpointBuilder = RegionEndpointBuilder()

    private fun createEndpointResolver(): EndpointResolver {
        return if (credentials.modId != null && credentials.cmdId != null) {
            val cl5Info = CL5Info(credentials.modId!!, credentials.cmdId!!, credentials.timeout)
            CL5EndpointResolver(cl5Info)
        } else {
            DefaultEndpointResolver()
        }
    }

    companion object {
        private const val MAX_PARTS = 10000
        private const val MULTIPART_THRESHOLD_SIZE = 10L
        private const val MIN_PART_SIZE = 10L
    }
}

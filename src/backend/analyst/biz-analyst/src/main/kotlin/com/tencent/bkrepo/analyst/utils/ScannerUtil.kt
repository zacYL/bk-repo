/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2023 Tencent.  All rights reserved.
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

package com.tencent.bkrepo.analyst.utils

import com.tencent.bkrepo.analyst.pojo.execution.KubernetesExecutionClusterProperties
import com.tencent.bkrepo.common.analysis.pojo.scanner.standard.StandardScanner
import com.tencent.bkrepo.common.analysis.pojo.scanner.utils.DockerUtils.determineDockerServer
import io.kubernetes.client.openapi.models.V1Secret

object ScannerUtil {

    fun getOrCreateSecret(scanner: StandardScanner, k8sClusterProp: KubernetesExecutionClusterProperties): V1Secret {
        with(scanner) {
            require(isPrivateImage(this))
            val helper = K8SHelper(k8sClusterProp)
            val secretName = scanner.name
            val dockerRegistryServer = determineDockerServer(image)
            return helper.getSecret(secretName) ?: helper.createSecret(
                secretName,
                dockerRegistryServer,
                dockerRegistryUsername!!,
                dockerRegistryPassword!!
            )
        }
    }

    fun isPrivateImage(scanner: StandardScanner): Boolean {
        with(scanner) {
            return dockerRegistryUsername != null &&
                dockerRegistryPassword != null
        }
    }
}

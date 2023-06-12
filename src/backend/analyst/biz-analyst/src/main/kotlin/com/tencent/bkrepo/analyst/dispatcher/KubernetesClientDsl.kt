/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.analyst.dispatcher

import io.kubernetes.client.custom.Quantity
import io.kubernetes.client.openapi.models.V1Container
import io.kubernetes.client.openapi.models.V1Job
import io.kubernetes.client.openapi.models.V1JobSpec
import io.kubernetes.client.openapi.models.V1LocalObjectReference
import io.kubernetes.client.openapi.models.V1ObjectMeta
import io.kubernetes.client.openapi.models.V1PodSpec
import io.kubernetes.client.openapi.models.V1PodTemplateSpec
import io.kubernetes.client.openapi.models.V1ResourceRequirements
import io.kubernetes.client.openapi.models.V1Secret

/**
 * 创建Job并配置
 */
inline fun v1Job(crossinline configuration: V1Job.() -> Unit): V1Job {
    return V1Job().apply(configuration)
}

/**
 * 创建Secret并配置
 */
inline fun v1Secret(crossinline configuration: V1Secret.() -> Unit): V1Secret {
    return V1Secret().apply(configuration)
}

/**
 * 创建V1LocalObjectReference并配置
 */
inline fun v1LocalObjectReference(
    crossinline configuration: V1LocalObjectReference.() -> Unit
): V1LocalObjectReference {
    return V1LocalObjectReference().apply(configuration)
}

/**
 * 配置Secret元数据
 */

inline fun V1Secret.metadata(crossinline configuration: V1ObjectMeta.() -> Unit) {
    metadata?.apply(configuration) ?: V1ObjectMeta().apply(configuration).also { metadata = it }
}

/**
 * 配置Job元数据
 */
inline fun V1Job.metadata(crossinline configuration: V1ObjectMeta.() -> Unit) {
    metadata?.apply(configuration) ?: V1ObjectMeta().apply(configuration).also { metadata = it }
}

/**
 * 配置Job执行方式
 */
inline fun V1Job.spec(crossinline configuration: V1JobSpec.() -> Unit) {
    spec?.apply(configuration) ?: V1JobSpec().apply(configuration).also { spec = it }
}

/**
 * 配置Job用于创建Pod的模板
 */
inline fun V1JobSpec.template(crossinline configuration: V1PodTemplateSpec.() -> Unit) {
    template?.apply(configuration) ?: V1PodTemplateSpec().apply(configuration).also { template = it }
}

/**
 * 配置Pod
 */
inline fun V1PodTemplateSpec.spec(crossinline configuration: V1PodSpec.() -> Unit) {
    spec?.apply(configuration) ?: V1PodSpec().apply(configuration).also { spec = it }
}

/**
 * 为Pod添加Container配置
 */
inline fun V1PodSpec.addContainerItem(crossinline configuration: V1Container.() -> Unit) {
    addContainersItem(V1Container().apply(configuration))
}

/**
 * 配置资源需求
 */
inline fun V1Container.resources(crossinline configuration: V1ResourceRequirements.() -> Unit) {
    resources?.apply(configuration) ?: V1ResourceRequirements().apply(configuration).also { resources = it }
}

/**
 * 声明最小所需资源
 */
fun V1ResourceRequirements.requests(cpu: Double, memory: Long, ephemeralStorage: Long) {
    requests(
        mapOf(
            "cpu" to Quantity("$cpu"),
            "memory" to Quantity("$memory"),
            "ephemeral-storage" to Quantity("$ephemeralStorage")
        )
    )
}

/**
 * 设置最大可用资源
 */
fun V1ResourceRequirements.limits(cpu: Double, memory: Long, ephemeralStorage: Long) {
    limits(
        mapOf(
            "cpu" to Quantity("$cpu"),
            "memory" to Quantity("$memory"),
            "ephemeral-storage" to Quantity("$ephemeralStorage")
        )
    )
}

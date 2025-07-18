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

package com.tencent.bkrepo.helm.metrics

import com.tencent.bkrepo.helm.constants.HELM_EVENT_TASK_ACTIVE_COUNT
import com.tencent.bkrepo.helm.constants.HELM_EVENT_TASK_ACTIVE_COUNT_DESC
import com.tencent.bkrepo.helm.constants.HELM_EVENT_TASK_COMPLETED_COUNT
import com.tencent.bkrepo.helm.constants.HELM_EVENT_TASK_COMPLETED_COUNT_DESC
import com.tencent.bkrepo.helm.constants.HELM_EVENT_TASK_QUEUE_SIZE
import com.tencent.bkrepo.helm.constants.HELM_EVENT_TASK_QUEUE_SIZE_DESC
import com.tencent.bkrepo.helm.constants.HELM_INDEX_REFRESH_TASK_ACTIVE_COUNT
import com.tencent.bkrepo.helm.constants.HELM_INDEX_REFRESH_TASK_ACTIVE_COUNT_DESC
import com.tencent.bkrepo.helm.constants.HELM_INDEX_REFRESH_TASK_COMPLETED_COUNT
import com.tencent.bkrepo.helm.constants.HELM_INDEX_REFRESH_TASK_COMPLETED_COUNT_DESC
import com.tencent.bkrepo.helm.constants.HELM_INDEX_REFRESH_TASK_QUEUE_SIZE
import com.tencent.bkrepo.helm.constants.HELM_INDEX_REFRESH_TASK_QUEUE_SIZE_DESC
import com.tencent.bkrepo.helm.pool.EventHandlerThreadPoolExecutor
import com.tencent.bkrepo.helm.pool.HelmThreadPoolExecutor
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import org.springframework.stereotype.Component

@Component
class HelmMetrics: MeterBinder {
    override fun bindTo(registry: MeterRegistry) {
        Gauge.builder(HELM_EVENT_TASK_ACTIVE_COUNT, EventHandlerThreadPoolExecutor.instance) {
            it.activeCount.toDouble()
        }.description(HELM_EVENT_TASK_ACTIVE_COUNT_DESC)
            .register(registry)

        Gauge.builder(HELM_EVENT_TASK_QUEUE_SIZE, EventHandlerThreadPoolExecutor.instance) { it.queue.size.toDouble() }
            .description(HELM_EVENT_TASK_QUEUE_SIZE_DESC)
            .register(registry)

        Gauge.builder(HELM_EVENT_TASK_COMPLETED_COUNT, EventHandlerThreadPoolExecutor.instance) {
            it.completedTaskCount.toDouble()
        }.description(HELM_EVENT_TASK_COMPLETED_COUNT_DESC)
            .register(registry)


        Gauge.builder(HELM_INDEX_REFRESH_TASK_ACTIVE_COUNT, HelmThreadPoolExecutor.instance) {
            it.activeCount.toDouble()
        }.description(HELM_INDEX_REFRESH_TASK_ACTIVE_COUNT_DESC)
            .register(registry)

        Gauge.builder(HELM_INDEX_REFRESH_TASK_QUEUE_SIZE, HelmThreadPoolExecutor.instance) { it.queue.size.toDouble() }
            .description(HELM_INDEX_REFRESH_TASK_QUEUE_SIZE_DESC)
            .register(registry)

        Gauge.builder(HELM_INDEX_REFRESH_TASK_COMPLETED_COUNT, HelmThreadPoolExecutor.instance) {
            it.completedTaskCount.toDouble()
        }.description(HELM_INDEX_REFRESH_TASK_COMPLETED_COUNT_DESC)
            .register(registry)
    }
}
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

package com.tencent.bkrepo.analyst.event.listener

import com.tencent.bkrepo.analyst.event.ScanTaskNotifyEvent
import com.tencent.bkrepo.analyst.pojo.ScanTaskStatus
import com.tencent.bkrepo.common.devops.conf.DevopsConf
import com.tencent.bkrepo.common.notify.pojo.enums.PlatformSmallBell
import com.tencent.bkrepo.common.notify.service.PlatformNotify
import com.tencent.bkrepo.common.service.condition.ConditionalOnDevops
import java.time.format.DateTimeFormatter
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component


@Component
@ConditionalOnDevops
class ScanTaskNotifyEventListener(
    private val devopsConf: DevopsConf,
    private val platformNotify: PlatformNotify
) {
    @Async
    @EventListener(ScanTaskNotifyEvent::class)
    fun listen(event: ScanTaskNotifyEvent) {
        with(event) {
            if (scanTask.projectId == null || scanPlan == null) {
                return
            }
            val templateCode = when (event.status) {
                ScanTaskStatus.STOPPED -> PlatformSmallBell.ARTIFACT_SCAN_STOPPED_TEMPLATE
                ScanTaskStatus.FINISHED -> PlatformSmallBell.ARTIFACT_SCAN_FINISH_TEMPLATE
                else -> return
            }
            val receivers = setOf(scanPlan.createdBy, scanPlan.lastModifiedBy, scanTask.createdBy)
            val bodyParams = mapOf(
                "name" to scanPlan.name,
                "completionTime" to completedTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                "url" to "${devopsConf.devopsHost}/console/repository/${scanTask.projectId}/repoScan/scanReport/${scanTask.planId}",
                "operator" to operator
            )
            platformNotify.sendPlatformNotify(
                scanTask.projectId,
                templateCode,
                receivers,
                bodyParams
            )
        }
    }
}

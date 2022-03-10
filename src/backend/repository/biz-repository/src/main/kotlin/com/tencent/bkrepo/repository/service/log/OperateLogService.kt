/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.repository.service.log

import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.artifact.event.base.ArtifactEvent
import com.tencent.bkrepo.repository.pojo.event.EventCreateRequest
import com.tencent.bkrepo.repository.pojo.log.OperateLogPojo
import com.tencent.bkrepo.repository.pojo.log.OperateLogResponse
import java.time.LocalDateTime

interface OperateLogService {

    /**
     * 异步保存事件
     * @param event 事件
     * @param address 客户端地址，需要提前传入，因为异步情况下无法获取request
     */
    fun saveEventAsync(event: ArtifactEvent, address: String)

    fun saveMultiEventAsync(events: List<ArtifactEvent>, address: String)

    fun saveEventRequest(request: EventCreateRequest)

    fun page(
        type: ResourceType?,
        projectId: String?,
        repoName: String?,
        operator: String?,
        startTime: String?,
        endTime: String?,
        pageNumber: Int,
        pageSize: Int
    ): Page<OperateLogResponse?>

    fun uploads(
        projectId: String?,
        repoName: String?,
        latestWeek: Boolean?
    ): Long

    fun uploadsByDay(
        projectId: String?,
        repoName: String?,
        days: Long?
    ): Long

    fun downloads(
        projectId: String?,
        repoName: String?,
        latestWeek: Boolean?
    ): Long

    /**
     * 查询指定时间之后的操作记录
     */
    fun operateLogLimitByTime(time: LocalDateTime, pageNumber: Int, pageSize: Int): List<OperateLogPojo>
}

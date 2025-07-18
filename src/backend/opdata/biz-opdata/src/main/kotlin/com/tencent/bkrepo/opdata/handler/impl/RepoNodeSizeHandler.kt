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

package com.tencent.bkrepo.opdata.handler.impl

import com.tencent.bkrepo.opdata.config.OpProjectMetricsProperties
import com.tencent.bkrepo.opdata.constant.TO_GIGABYTE
import com.tencent.bkrepo.opdata.handler.QueryHandler
import com.tencent.bkrepo.opdata.pojo.Target
import com.tencent.bkrepo.opdata.pojo.enums.Metrics
import com.tencent.bkrepo.opdata.repository.ProjectMetricsRepository
import com.tencent.bkrepo.opdata.util.StatDateUtil
import org.springframework.stereotype.Component

/**
 * 项目节点容量统计
 */
@Component
class RepoNodeSizeHandler(
    private val projectMetricsRepository: ProjectMetricsRepository,
    private val opProjectMetricsProperties: OpProjectMetricsProperties,
    ) : QueryHandler {

    override val metric: Metrics get() = Metrics.REPONODESIZE

    override fun handle(target: Target, result: MutableList<Any>): List<Any> {
        val projects = projectMetricsRepository.findAllByCreatedDate(StatDateUtil.getStatDate())
        val tmpMap = HashMap<String, Long>()
        projects.forEach { it ->
            val projectId = it.projectId
            it.repoMetrics.forEach {
                val repoName = it.repoName
                val gbSize = it.size / TO_GIGABYTE
                if (gbSize != 0L) {
                    tmpMap["$projectId-$repoName"] = gbSize
                }
            }
        }
        val top = getTopValue(target, opProjectMetricsProperties.top)
        return convToDisplayData(tmpMap, result, top)
    }
}

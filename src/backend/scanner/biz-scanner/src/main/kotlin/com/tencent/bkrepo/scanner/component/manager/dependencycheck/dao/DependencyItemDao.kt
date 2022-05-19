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

package com.tencent.bkrepo.scanner.component.manager.dependencycheck.dao

import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.scanner.pojo.scanner.dependencycheck.result.DependencyItem
import com.tencent.bkrepo.scanner.component.manager.arrowhead.dao.ResultItemDao
import com.tencent.bkrepo.scanner.component.manager.dependencycheck.model.TDependencyItem
import com.tencent.bkrepo.scanner.pojo.request.ArrowheadLoadResultArguments
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.stereotype.Repository

@Repository
class DependencyItemDao : ResultItemDao<TDependencyItem>() {
    override fun customizePageBy(criteria: Criteria, arguments: ArrowheadLoadResultArguments): Criteria {
        if (arguments.vulnerabilityLevels.isNotEmpty()) {
            criteria.and(dataKey(DependencyItem::severity.name)).inValues(arguments.vulnerabilityLevels)
        }
        if (arguments.vulIds.isNotEmpty()) {
            // todo
            // criteria.andOperator(buildVulIdCriteria(arguments.vulIds))
            criteria.and(dataKey(DependencyItem::cveId.name)).inValues(arguments.vulIds)
        }
        logger.info("DependencyItemDao customizePageBy criteria:${criteria.toJsonString()}")
        return criteria
    }

    private fun dataKey(name: String) = "${TDependencyItem::data.name}.$name"

    companion object {
        private val logger = LoggerFactory.getLogger(DependencyItemDao::class.java)
    }
}

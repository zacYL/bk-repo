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

package com.tencent.bkrepo.scanner.component.manager.scancode.dao

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.query.model.PageLimit
import com.tencent.bkrepo.common.scanner.pojo.scanner.scanCodeCheck.result.ScancodeItem
import com.tencent.bkrepo.scanner.component.manager.ResultItemDao
import com.tencent.bkrepo.scanner.component.manager.scancode.model.TScancodeItem
import com.tencent.bkrepo.scanner.pojo.request.scancodetoolkit.ScancodeToolkitResultArguments
import org.springframework.data.domain.PageRequest
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.stereotype.Repository

@Repository
class ScancodeItemDao : ResultItemDao<TScancodeItem>() {
    fun customizePageBy(criteria: Criteria, arguments: ScancodeToolkitResultArguments): Criteria {
        if (!arguments.riskLevels.isNullOrEmpty()) {
            criteria.and(dataKey(ScancodeItem::riskLevel.name)).inValues(arguments.riskLevels!!)
        }
        if (!arguments.licenseIds.isNullOrEmpty()) {
            criteria.and(dataKey(ScancodeItem::licenseId.name)).inValues(arguments.licenseIds!!)
        }
        logger.info("ScancodeItemDao customizePageBy criteria:${criteria.toJsonString()}")
        return criteria
    }

    private fun dataKey(name: String) = "${TScancodeItem::data.name}.$name"

    fun pageOf(
            credentialsKey: String?,
            sha256: String,
            scanner: String,
            pageLimit: PageLimit,
            arguments: ScancodeToolkitResultArguments
    ): Page<TScancodeItem> {
        val criteria = buildCriteria(credentialsKey, sha256, scanner)
        customizePageBy(criteria, arguments)
        val query = Query(criteria).with(PageRequest.of(pageLimit.pageNumber - 1, pageLimit.pageSize))
        val total = count(Query.of(query).limit(0).skip(0))
        val data = find(query)
        return Page(pageLimit.pageNumber, pageLimit.pageSize, total, data)
    }
}

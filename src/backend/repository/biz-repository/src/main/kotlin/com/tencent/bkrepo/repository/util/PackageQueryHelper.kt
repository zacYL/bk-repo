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

package com.tencent.bkrepo.repository.util

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.query.util.MongoEscapeUtils
import com.tencent.bkrepo.repository.constant.DATE
import com.tencent.bkrepo.repository.constant.PACKAGE_KEY
import com.tencent.bkrepo.repository.constant.PACKAGE_VERSION
import com.tencent.bkrepo.repository.constant.PROJECT_ID
import com.tencent.bkrepo.repository.constant.REPO_NAME
import com.tencent.bkrepo.repository.model.TPackage
import com.tencent.bkrepo.repository.model.TPackageVersion
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import java.time.LocalDate

/**
 * 查询条件构造工具
 */
object PackageQueryHelper {

    // package
    fun packageQuery(projectId: String, repoName: String, key: String): Query {
        val criteria = where(TPackage::projectId).isEqualTo(projectId)
            .and(TPackage::repoName).isEqualTo(repoName)
            .and(TPackage::key).isEqualTo(key)
        return Query(criteria)
    }

    fun packageListQuery(projectId: String, repoName: String, packageName: String?): Query {
        return Query(packageListCriteria(projectId, repoName, packageName))
    }

    // version
    fun versionQuery(packageId: String, name: String? = null, tag: String? = null): Query {
        val criteria = where(TPackageVersion::packageId).isEqualTo(packageId)
            .apply {
                name?.let { and(TPackageVersion::name).isEqualTo(name) }
                tag?.let { and(TPackageVersion::tags).inValues(tag) }
            }
        return Query(criteria)
    }

    fun versionListQuery(
        packageId: String,
        name: String? = null,
        stageTag: List<String>? = null,
        sortProperty: String? = null
    ): Query {
        return Query(versionListCriteria(packageId, name, stageTag))
            .with(Sort.by(Sort.Order(Sort.Direction.DESC, sortProperty ?: TPackageVersion::createdDate.name)))
    }

    fun versionLatestQuery(packageId: String, sortProperty: String? = null): Query {
        return versionListQuery(packageId, sortProperty = sortProperty).limit(1)
    }

    fun versionQuery(packageId: String, versionList: List<String>): Query {
        val criteria = where(TPackageVersion::packageId).isEqualTo(packageId)
        if (versionList.isNotEmpty()) {
            criteria.and(TPackageVersion::name).inValues(versionList)
        }
        return Query(criteria)
    }

    fun recordQuery(projectId: String, fromDate: LocalDate, toDate: LocalDate): Query {
        val criteria = recordCriteria(
            projectId = projectId,
            fromDate = fromDate,
            toDate = toDate
        )
        return Query(criteria)
    }

    fun recordCriteria(
        projectId: String,
        repoName: String? = null,
        packageKey: String? = null,
        packageVersion: String? = null,
        eqDate: LocalDate? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null
    ): Criteria {
        val criteria = Criteria.where(PROJECT_ID).isEqualTo(projectId)
            .apply {
                repoName?.let { and(REPO_NAME).isEqualTo(it) }
                packageKey?.let { and(PACKAGE_KEY).isEqualTo(it) }
                packageVersion?.let { and(PACKAGE_VERSION).isEqualTo(it) }
                eqDate?.let { and(DATE).isEqualTo(it.toString()) }
            }
        if (fromDate != null && toDate != null) {
            if (fromDate.isAfter(toDate)) {
                throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, "date range")
            }
            val fromDateString = fromDate.toString()
            val toDateString = toDate.toString()
            if (fromDate.isEqual(toDate)) {
                criteria.and(DATE).isEqualTo(fromDateString)
            } else {
                criteria.and(DATE).gte(fromDateString).lte(toDateString)
            }
        }
        return criteria
    }

    private fun packageListCriteria(projectId: String, repoName: String, packageName: String?): Criteria {
        return where(TPackage::projectId).isEqualTo(projectId)
            .and(TPackage::repoName).isEqualTo(repoName)
            .apply {
                packageName?.let {
                    val escapedValue = MongoEscapeUtils.escapeRegexExceptWildcard(it)
                    val regexPattern = escapedValue.replace("*", ".*")
                    and(TPackage::name).regex("^$regexPattern", "i") }
            }
    }

    private fun versionListCriteria(packageId: String, name: String? = null, stageTag: List<String>? = null): Criteria {
        return where(TPackageVersion::packageId).isEqualTo(packageId)
            .apply {
                name?.let {
                    val escapedValue = MongoEscapeUtils.escapeRegexExceptWildcard(it)
                    val regexPattern = escapedValue.replace("*", ".*")
                    and(TPackageVersion::name).regex(regexPattern) }
            }.apply {
                if (!stageTag.isNullOrEmpty()) {
                    and(TPackageVersion::stageTag).all(stageTag)
                }
            }
    }
}

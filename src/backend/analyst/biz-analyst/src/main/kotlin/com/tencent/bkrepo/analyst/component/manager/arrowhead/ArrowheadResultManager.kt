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

package com.tencent.bkrepo.analyst.component.manager.arrowhead

import com.tencent.bkrepo.analyst.component.manager.AbstractScanExecutorResultManager
import com.tencent.bkrepo.analyst.component.manager.arrowhead.dao.ApplicationItemDao
import com.tencent.bkrepo.analyst.component.manager.arrowhead.dao.CheckSecItemDao
import com.tencent.bkrepo.analyst.component.manager.arrowhead.dao.CveSecItemDao
import com.tencent.bkrepo.analyst.component.manager.arrowhead.dao.SensitiveItemDao
import com.tencent.bkrepo.analyst.component.manager.arrowhead.model.TApplicationItem
import com.tencent.bkrepo.analyst.component.manager.arrowhead.model.TCheckSecItem
import com.tencent.bkrepo.analyst.component.manager.arrowhead.model.TCveSecItem
import com.tencent.bkrepo.analyst.component.manager.arrowhead.model.TSensitiveItem
import com.tencent.bkrepo.analyst.component.manager.knowledgebase.KnowledgeBase
import com.tencent.bkrepo.analyst.component.manager.knowledgebase.TCve
import com.tencent.bkrepo.analyst.component.manager.knowledgebase.TLicense
import com.tencent.bkrepo.analyst.message.ScannerMessageCode
import com.tencent.bkrepo.analyst.pojo.request.ArrowheadLoadResultArguments
import com.tencent.bkrepo.analyst.pojo.request.LoadResultArguments
import com.tencent.bkrepo.analyst.pojo.request.SaveResultArguments
import com.tencent.bkrepo.common.analysis.pojo.scanner.ScanExecutorResult
import com.tencent.bkrepo.common.analysis.pojo.scanner.Scanner
import com.tencent.bkrepo.common.analysis.pojo.scanner.arrowhead.ApplicationItem
import com.tencent.bkrepo.common.analysis.pojo.scanner.arrowhead.ArrowheadScanExecutorResult
import com.tencent.bkrepo.common.analysis.pojo.scanner.arrowhead.ArrowheadScanner
import com.tencent.bkrepo.common.analysis.pojo.scanner.arrowhead.CheckSecItem
import com.tencent.bkrepo.common.analysis.pojo.scanner.arrowhead.CveSecItem
import com.tencent.bkrepo.common.analysis.pojo.scanner.arrowhead.SensitiveItem
import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.query.model.PageLimit
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component(ArrowheadScanner.TYPE)
class ArrowheadResultManager @Autowired constructor(
    private val checkSecItemDao: CheckSecItemDao,
    private val applicationItemDao: ApplicationItemDao,
    private val sensitiveItemDao: SensitiveItemDao,
    private val cveSecItemDao: CveSecItemDao,
    private val knowledgeBase: KnowledgeBase
) : AbstractScanExecutorResultManager() {

    @Transactional(rollbackFor = [Throwable::class])
    override fun save(
        credentialsKey: String?,
        sha256: String,
        scanner: Scanner,
        result: ScanExecutorResult,
        arguments: SaveResultArguments?
    ) {
        result as ArrowheadScanExecutorResult
        scanner as ArrowheadScanner
        val scannerName = scanner.name

        result.checkSecItems
            .map { convert<CheckSecItem, TCheckSecItem>(credentialsKey, sha256, scannerName, it) }
            .run { replace(credentialsKey, sha256, scannerName, checkSecItemDao, this) }

        replaceApplicationItems(credentialsKey, sha256, scannerName, result.applicationItems)

        result.sensitiveItems
            .map { convert<SensitiveItem, TSensitiveItem>(credentialsKey, sha256, scannerName, it) }
            .run { replace(credentialsKey, sha256, scannerName, sensitiveItemDao, this) }

        replaceCveItems(credentialsKey, sha256, scannerName, result.cveSecItems)
    }

    override fun load(
        credentialsKey: String?,
        sha256: String,
        scanner: Scanner,
        arguments: LoadResultArguments?
    ): Any? {
        scanner as ArrowheadScanner
        arguments as ArrowheadLoadResultArguments
        val pageLimit = arguments.pageLimit
        val type = arguments.reportType

        val page = when (type) {
            CheckSecItem.TYPE -> checkSecItemDao
            ApplicationItem.TYPE -> return loadApplicationItems(credentialsKey, sha256, scanner, pageLimit, arguments)
            SensitiveItem.TYPE -> sensitiveItemDao
            CveSecItem.TYPE -> return loadCveItems(credentialsKey, sha256, scanner, pageLimit, arguments)
            else -> {
                throw ErrorCodeException(
                    messageCode = ScannerMessageCode.SCANNER_RESULT_TYPE_INVALID,
                    status = HttpStatus.BAD_REQUEST,
                    params = arrayOf(type)
                )
            }
        }.run { pageBy(credentialsKey, sha256, scanner.name, pageLimit, arguments) }

        return Page(page.pageNumber, page.pageSize, page.totalRecords, page.records.map { it.data })
    }

    override fun clean(credentialsKey: String?, sha256: String, scannerName: String): Long {
        var deletedCount = 0L
        deletedCount += applicationItemDao.deleteBy(credentialsKey, sha256, scannerName).deletedCount
        deletedCount += checkSecItemDao.deleteBy(credentialsKey, sha256, scannerName).deletedCount
        deletedCount += cveSecItemDao.deleteBy(credentialsKey, sha256, scannerName).deletedCount
        deletedCount += sensitiveItemDao.deleteBy(credentialsKey, sha256, scannerName).deletedCount
        return deletedCount
    }

    private fun loadApplicationItems(
        credentialsKey: String?,
        sha256: String,
        scanner: Scanner,
        pageLimit: PageLimit,
        arguments: ArrowheadLoadResultArguments
    ): Page<ApplicationItem> {
        val page = applicationItemDao.pageBy(credentialsKey, sha256, scanner.name, pageLimit, arguments)
        val licenseNames = page.records.filter { it.data.licenseName != null }.map { it.data.licenseName!! }
        val licenses = knowledgeBase.findLicense(licenseNames).associateBy { it.name }
        val records = page.records.map { Converter.convert(it, licenses[it.data.licenseName]) }
        return Page(page.pageNumber, page.pageSize, page.totalRecords, records)
    }

    private fun replaceApplicationItems(
        credentialsKey: String?,
        sha256: String,
        scanner: String,
        applicationItems: List<ApplicationItem>
    ) {
        val licenses = HashSet<TLicense>()
        val tApplicationItems = ArrayList<TApplicationItem>(applicationItems.size)

        applicationItems
            .asSequence()
            .filter { it.license != null }
            .forEach { item ->
                licenses.add(Converter.convertToLicense(item.license!!))
                tApplicationItems.add(
                    TApplicationItem(
                        credentialsKey = credentialsKey,
                        sha256 = sha256,
                        scanner = scanner,
                        data = Converter.convert(item)
                    )
                )
            }
        if (licenses.isNotEmpty()) {
            knowledgeBase.saveLicenses(licenses)
        }
        replace(credentialsKey, sha256, scanner, applicationItemDao, tApplicationItems)
    }

    private fun loadCveItems(
        credentialsKey: String?,
        sha256: String,
        scanner: Scanner,
        pageLimit: PageLimit,
        arguments: ArrowheadLoadResultArguments
    ): Page<CveSecItem> {
        val page = cveSecItemDao.pageBy(credentialsKey, sha256, scanner.name, pageLimit, arguments)
        val pocIds = page.records.map { it.data.pocId }
        val cveMap = knowledgeBase.findByPocId(pocIds).associateBy { it.pocId }
        val records = page.records.map { Converter.convert(it, cveMap[it.data.pocId]) }
        return Page(page.pageNumber, page.pageSize, page.totalRecords, records)
    }

    private fun replaceCveItems(
        credentialsKey: String?,
        sha256: String,
        scanner: String,
        cveItems: List<CveSecItem>
    ) {
        val cveSet = HashSet<TCve>(cveItems.size)
        val tCveItems = ArrayList<TCveSecItem>(cveItems.size)

        cveItems
            .asSequence()
            .forEach {
                cveSet.add(Converter.convertToCve(it))
                tCveItems.add(
                    TCveSecItem(
                        credentialsKey = credentialsKey,
                        sha256 = sha256,
                        scanner = scanner,
                        data = Converter.convert(it)
                    )
                )
            }

        if (cveSet.isNotEmpty()) {
            knowledgeBase.saveCve(cveSet)
        }
        replace(credentialsKey, sha256, scanner, cveSecItemDao, tCveItems)
    }
}

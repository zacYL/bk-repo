package com.tencent.bkrepo.analyst.model

import com.alibaba.excel.annotation.ExcelProperty
import com.alibaba.excel.annotation.write.style.ColumnWidth

data class LeakDetailExport(
    @ColumnWidth(30)
    @ExcelProperty("{excel.leak-detail.vul-id}", order = 0)
    val vulId: String,
    @ColumnWidth(30)
    @ExcelProperty("{excel.leak-detail.severity}", order = 1)
    val severity: String,
    @ColumnWidth(30)
    @ExcelProperty("{excel.leak-detail.pkg-name}", order = 2)
    val pkgName: String,
    @ColumnWidth(30)
    @ExcelProperty("{excel.leak-detail.installed-version}", order = 3)
    val installedVersion: String,
    @ColumnWidth(45)
    @ExcelProperty("{excel.leak-detail.vulnerability-name}", order = 4)
    val vulnerabilityName: String,
    @ColumnWidth(90)
    @ExcelProperty("{excel.leak-detail.description}", order = 5)
    val description: String? = null,
    @ColumnWidth(120)
    @ExcelProperty("{excel.leak-detail.reference}", order = 6)
    val reference: String? = null,
    @ColumnWidth(45)
    @ExcelProperty("{excel.leak-detail.official-solution}", order = 7)
    val officialSolution: String? = null
)

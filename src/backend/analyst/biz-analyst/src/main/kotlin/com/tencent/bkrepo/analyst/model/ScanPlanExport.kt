package com.tencent.bkrepo.analyst.model

import com.alibaba.excel.annotation.ExcelProperty
import com.alibaba.excel.annotation.write.style.ColumnWidth

data class ScanPlanExport(
    @ColumnWidth(30)
    @ExcelProperty(value = ["{excel.scan-plan.name}"], order = 0)
    val name: String,
    @ColumnWidth(30)
    @ExcelProperty(value = ["{excel.scan-plan.version-or-full-path}"], order = 1)
    val versionOrFullPath: String,
    @ColumnWidth(20)
    @ExcelProperty(value = ["{excel.scan-plan.repo-name}"], order = 2)
    val repoName: String,
    @ColumnWidth(20)
    @ExcelProperty(value = ["{excel.scan-plan.status}"], order = 3)
    val status: String,
    @ColumnWidth(30)
    @ExcelProperty(value = ["{excel.scan-plan.finish-time}"], order = 4)
    val finishTime: String,
    @ColumnWidth(30)
    @ExcelProperty(value = ["{excel.scan-plan.duration}"], order = 5)
    val duration: Long? = null,
    @ColumnWidth(50)
    @ExcelProperty(value = ["{excel.scan-plan.critical}"], order = 6)
    val critical: Long? = null,
    @ColumnWidth(50)
    @ExcelProperty(value = ["{excel.scan-plan.high}"], order = 7)
    val high: Long? = null,
    @ColumnWidth(50)
    @ExcelProperty(value = ["{excel.scan-plan.medium}"], order = 8)
    val medium: Long? = null,
    @ColumnWidth(50)
    @ExcelProperty(value = ["{excel.scan-plan.low}"], order = 9)
    val low: Long? = null,
    @ColumnWidth(35)
    @ExcelProperty(value = ["{excel.scan-plan.total}"], order = 10)
    val total: Long? = null,
    @ColumnWidth(40)
    @ExcelProperty(value = ["{excel.scan-plan.un-recommend}"], order = 11)
    val unRecommend: Long? = null,
    @ColumnWidth(40)
    @ExcelProperty(value = ["{excel.scan-plan.un-known}"], order = 12)
    val unknown: Long? = null,
    @ColumnWidth(40)
    @ExcelProperty(value = ["{excel.scan-plan.un-compliance}"], order = 13)
    val unCompliance: Long? = null,
)

package com.tencent.bkrepo.analyst.model

import com.alibaba.excel.annotation.ExcelProperty
import com.alibaba.excel.annotation.write.style.ColumnWidth

data class LicenseScanDetailExport(
    @ColumnWidth(45)
    @ExcelProperty("{excel.license-scan-detail.full-name}", order = 0)
    val fullName: String,
    @ColumnWidth(45)
    @ExcelProperty("{excel.license-scan-detail.dependent-path}", order = 1)
    val dependentPath: String,
    @ColumnWidth(30)
    @ExcelProperty("{excel.license-scan-detail.osi}", order = 2)
    val osi: String,
    @ColumnWidth(30)
    @ExcelProperty("{excel.license-scan-detail.fsf}", order = 3)
    val fsf: String,
    @ColumnWidth(30)
    @ExcelProperty("{excel.license-scan-detail.deprecated}", order = 4)
    val deprecated: String,
    @ColumnWidth(30)
    @ExcelProperty("{excel.license-scan-detail.compliance}", order = 5)
    val compliance: String?,
    @ColumnWidth(60)
    @ExcelProperty("{excel.license-scan-detail.description}", order = 6)
    val description: String
)

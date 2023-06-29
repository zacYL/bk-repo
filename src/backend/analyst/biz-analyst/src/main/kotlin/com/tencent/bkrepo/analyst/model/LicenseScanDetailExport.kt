package com.tencent.bkrepo.analyst.model

import com.alibaba.excel.annotation.ExcelProperty
import com.alibaba.excel.annotation.write.style.ColumnWidth

data class LicenseScanDetailExport(
    @ColumnWidth(45)
    @ExcelProperty("许可证名称全称")
    val fullName: String,
    @ColumnWidth(45)
    @ExcelProperty("依赖路径")
    val dependentPath: String,
    @ColumnWidth(15)
    @ExcelProperty("OSI认证")
    val osi: String,
    @ColumnWidth(15)
    @ExcelProperty("FSF开源")
    val fsf: String,
    @ColumnWidth(15)
    @ExcelProperty("使用状态")
    val deprecated: String,
    @ColumnWidth(15)
    @ExcelProperty("合规性")
    val compliance: String?,
    @ColumnWidth(60)
    @ExcelProperty("证书信息")
    val description: String
)

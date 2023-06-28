package com.tencent.bkrepo.analyst.model

import com.alibaba.excel.annotation.ExcelProperty

data class LicenseScanDetailExport(
    @ExcelProperty("许可证名称全称")
    val fullName: String,
    @ExcelProperty("依赖路径")
    val dependentPath: String,
    @ExcelProperty("OSI认证")
    val osi: String,
    @ExcelProperty("FSF开源")
    val fsf: String,
    @ExcelProperty("使用状态")
    val deprecated: String,
    @ExcelProperty("合规性")
    val compliance: String?,
    @ExcelProperty("证书信息")
    val description: String
)

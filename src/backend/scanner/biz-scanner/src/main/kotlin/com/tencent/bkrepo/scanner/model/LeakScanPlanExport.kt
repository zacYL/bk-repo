package com.tencent.bkrepo.scanner.model

import com.alibaba.excel.annotation.ExcelProperty

data class LeakScanPlanExport(
    //TODO 漏洞导出excel数据模型，
    // 需要导出的字段，@ExcelProperty("制品名称")：字段在 excel 中的表头
    @ExcelProperty("制品名称")
    val name: String
)

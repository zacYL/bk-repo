package com.tencent.bkrepo.analyst.utils

import com.alibaba.excel.metadata.Head
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder
import com.alibaba.excel.write.metadata.holder.WriteTableHolder
import com.alibaba.excel.write.metadata.style.WriteCellStyle
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy
import com.tencent.bkrepo.common.service.util.LocaleMessageUtils
import org.apache.poi.ss.usermodel.Row
import org.springframework.util.PropertyPlaceholderHelper

/**
 * 通过重写 HorizontalCellStyleStrategy 来实现动态表头修改 + 单元格格式修改
 */
class I18nHeaderCellWriteHandler(
    headWriteCellStyle: WriteCellStyle,
    contentWriteCellStyle: WriteCellStyle
) : HorizontalCellStyleStrategy(headWriteCellStyle, contentWriteCellStyle) {

    /**
     * 国际化翻译
     */
    private val placeholderResolver = PropertyPlaceholderHelper.PlaceholderResolver { placeholderName ->
        LocaleMessageUtils.getLocalizedMessage(placeholderName)
    }

    /**
     * 占位符处理
     */
    private val propertyPlaceholderHelper = PropertyPlaceholderHelper("{", "}")

    override fun beforeCellCreate(
        writeSheetHolder: WriteSheetHolder?,
        writeTableHolder: WriteTableHolder?,
        row: Row?,
        head: Head,
        columnIndex: Int?,
        relativeRowIndex: Int?,
        isHead: Boolean?
    ) {
        if (isHead == true) {
            head.headNameList.takeIf { it.isNotEmpty() }?.let { originList ->
                head.headNameList = originList.map { headName ->
                    propertyPlaceholderHelper.replacePlaceholders(headName, placeholderResolver)
                }
            }
        }
    }
}

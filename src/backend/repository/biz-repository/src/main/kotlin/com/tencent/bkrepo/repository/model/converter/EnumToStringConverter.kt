package com.tencent.bkrepo.repository.model.converter

import com.alibaba.excel.converters.Converter
import com.alibaba.excel.metadata.GlobalConfiguration
import com.alibaba.excel.metadata.data.WriteCellData
import com.alibaba.excel.metadata.property.ExcelContentProperty

class EnumToStringConverter<T : Enum<T>> : Converter<T> {

    override fun supportJavaTypeKey() = Enum::class.java

    override fun convertToExcelData(
        value: T?,
        contentProperty: ExcelContentProperty,
        globalConfiguration: GlobalConfiguration
    ) = WriteCellData<String>(value?.name.orEmpty())

}
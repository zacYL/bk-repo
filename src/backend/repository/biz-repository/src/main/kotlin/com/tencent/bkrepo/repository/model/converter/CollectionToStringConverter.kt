package com.tencent.bkrepo.repository.model.converter

import com.alibaba.excel.converters.Converter
import com.alibaba.excel.metadata.GlobalConfiguration
import com.alibaba.excel.metadata.data.WriteCellData
import com.alibaba.excel.metadata.property.ExcelContentProperty

class CollectionToStringConverter : Converter<Collection<String>> {

    override fun supportJavaTypeKey() = Collection::class.java

    override fun convertToExcelData(
        value: Collection<String>?,
        contentProperty: ExcelContentProperty,
        globalConfiguration: GlobalConfiguration
    ) = WriteCellData<String>(value?.joinToString().orEmpty())

}
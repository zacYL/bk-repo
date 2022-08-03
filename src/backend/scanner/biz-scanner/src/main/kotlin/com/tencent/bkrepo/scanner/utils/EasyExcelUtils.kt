package com.tencent.bkrepo.scanner.utils

import com.alibaba.excel.EasyExcel
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.scanner.exception.ExportReportException
import org.slf4j.LoggerFactory
import java.net.URLEncoder
import java.time.LocalDateTime

object EasyExcelUtils {
    private val logger = LoggerFactory.getLogger(EasyExcelUtils::class.java)

    fun download(data: Collection<*>, name: String, dataClass: Class<*>) {
        try {
            val response = HttpContextHolder.getResponse()
            response.contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            response.characterEncoding = "utf-8"
            val date = LocalDateTime.now()
            val fileName = URLEncoder.encode("$name-repo-$date", "UTF-8")
            response.setHeader(
                "Content-disposition",
                "attachment;filename*=utf-8''$fileName.xlsx"
            )
            EasyExcel.write(response.outputStream, dataClass).sheet(fileName).doWrite(data)
        } catch (e: Exception) {
            logger.error("download excel fail:[$e]")
            throw ExportReportException()
        }
    }
}

package com.tencent.bkrepo.cocoapods.service

import com.tencent.bkrepo.cocoapods.constant.CLIENT_PLUGIN_NAME
import com.tencent.bkrepo.cocoapods.constant.CLIENT_PLUGIN_PATH
import com.tencent.bkrepo.common.api.constant.HttpHeaders.CONTENT_DISPOSITION
import com.tencent.bkrepo.common.api.constant.MediaTypes.APPLICATION_OCTET_STREAM
import com.tencent.bkrepo.common.artifact.constant.CONTENT_DISPOSITION_TEMPLATE
import com.tencent.bkrepo.common.artifact.exception.ArtifactNotFoundException
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import org.springframework.stereotype.Service

@Service
class CocoapodsClientService {

    fun downloadClientPlugin() {
        val ips = this.javaClass.classLoader.getResourceAsStream(CLIENT_PLUGIN_PATH)
            ?: throw ArtifactNotFoundException(CLIENT_PLUGIN_NAME)
        val response = HttpContextHolder.getResponse()

        response.contentType = APPLICATION_OCTET_STREAM
        response.setHeader(CONTENT_DISPOSITION, CONTENT_DISPOSITION_TEMPLATE.format(CLIENT_PLUGIN_NAME, CLIENT_PLUGIN_NAME))
        response.outputStream.use { outputStream ->
            ips.use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        response.outputStream.flush()
    }
}

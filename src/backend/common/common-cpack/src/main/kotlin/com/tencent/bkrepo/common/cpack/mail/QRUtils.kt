package com.tencent.bkrepo.common.cpack.mail

import cn.hutool.core.img.ImgUtil
import cn.hutool.extra.qrcode.QrCodeUtil
import cn.hutool.extra.qrcode.QrConfig
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H

object QRUtils {

    private const val QRCODE_HEIGHT = 100
    private const val QRCODE_WIDTH = 100

    fun getQRCodeBase64(shortUrl: String): String {
        val qrConfig = QrConfig(QRCODE_WIDTH, QRCODE_HEIGHT)
        qrConfig.margin = 0
        qrConfig.errorCorrection = H
        return QrCodeUtil.generateAsBase64(shortUrl, qrConfig, ImgUtil.IMAGE_TYPE_PNG)
    }
}

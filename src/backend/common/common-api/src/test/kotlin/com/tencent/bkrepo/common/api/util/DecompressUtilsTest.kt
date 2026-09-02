package com.tencent.bkrepo.common.api.util

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class DecompressUtilsTest {

    @Test
    fun getContentWithinLimit() {
        val zip = zipBytes("pkg/metadata.json", "ok".toByteArray())
        val content = DecompressUtils.getContent(
            ZipArchiveInputStream(zip.inputStream()),
            "metadata.json",
            maxSize = 16
        )
        Assertions.assertEquals("ok", content)
    }

    @Test
    fun getContentExceedsLimit() {
        val zip = zipBytes("pkg/metadata.json", ByteArray(32) { 'a'.code.toByte() })
        Assertions.assertThrows(ErrorCodeException::class.java) {
            DecompressUtils.getContent(
                ZipArchiveInputStream(zip.inputStream()),
                "metadata.json",
                maxSize = 16
            )
        }
    }

    private fun zipBytes(fileName: String, content: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        ZipArchiveOutputStream(out).use { zos ->
            zos.putArchiveEntry(ZipArchiveEntry(fileName))
            zos.write(content)
            zos.closeArchiveEntry()
        }
        return out.toByteArray()
    }
}

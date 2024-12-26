package com.tencent.bkrepo.cocoapods.utils

import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.tika.Tika
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object FileUtil {
    fun detectFileType(file: File): String {
        val tika = Tika()
        return tika.detect(file)
    }

    fun detectFileType(ips: InputStream): String {
        val tika = Tika()
        return tika.detect(ips)
    }

    fun copyDirectory(sourcePath: String, targetDir: File) {
        val sourceDir = File(sourcePath)

        if (!sourceDir.exists() || !sourceDir.isDirectory) {
            throw IllegalArgumentException("Source path does not exist or is not a directory: $sourcePath")
        }

        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        // 遍历源目录并复制文件和子目录
        sourceDir.walk().forEach { sourceFile ->
            val relativePath = sourceDir.toPath().relativize(sourceFile.toPath())
            val targetFile = targetDir.toPath().resolve(relativePath).toFile()

            if (sourceFile.isDirectory) {
                targetFile.mkdirs()
            } else {
                Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    fun buildTempDir(child:String): File {
        // 临时解压目录
        return File(System.getProperty("java.io.tmpdir"), child)
    }
}

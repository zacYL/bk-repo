import com.tencent.bkrepo.cocoapods.model.TCocoapodsRemotePackage
import com.tencent.bkrepo.cocoapods.pojo.enums.PodSpecType
import com.tencent.bkrepo.cocoapods.utils.CocoapodsUtil
import com.tencent.bkrepo.cocoapods.utils.PathUtil.generateCachePath
import com.tencent.bkrepo.common.api.constant.MediaTypes.APPLICATION_GZIP
import com.tencent.bkrepo.common.api.constant.MediaTypes.APPLICATION_ZIP
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.math.ceil
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.slf4j.LoggerFactory

object ArchiveModifier {

    @Throws(IOException::class)
    fun modifyArchive(
        projectId: String, repoName: String,
        domain: String, archiveInputStream: InputStream,
        archiveOutputStream: OutputStream,
        archiveType: String, tempDir: File
    ): MutableList<Podspec> {

        // 解压文件
        when (archiveType) {
            APPLICATION_ZIP -> {
                zipDeCompressor(archiveInputStream, tempDir)
            }

            APPLICATION_GZIP -> {
                gzipPDeCompressor(archiveInputStream, tempDir)
            }

            else -> throw IllegalArgumentException("Unsupported archive type: $archiveType")
        }

        return modifyAndZip(tempDir, projectId, repoName, domain, archiveOutputStream)
    }

    private fun zipDeCompressor(archiveInputStream: InputStream, tempDir: File) {
        // 解压 ZIP 文件
        ZipInputStream(archiveInputStream).use { zis ->
            var entry: ZipEntry?
            while (zis.nextEntry.also { entry = it } != null) {
                mkdirAndWriteFile(
                    tempDir = tempDir,
                    name = entry!!.name,
                    isDirectory = entry!!.isDirectory,
                    input = zis,
                    gzip = false
                )
                zis.closeEntry()
            }
        }
    }

    private fun gzipPDeCompressor(archiveInputStream: InputStream, tempDir: File) {
        // 解压 TAR.GZ 文件
        GzipCompressorInputStream(archiveInputStream).use { gzipIn ->
            TarArchiveInputStream(gzipIn).use { tarIn ->
                var entry: TarArchiveEntry?
                while (tarIn.nextTarEntry.also { entry = it } != null) {
                    mkdirAndWriteFile(
                        tempDir = tempDir,
                        name = entry!!.name,
                        isDirectory = entry!!.isDirectory,
                        input = tarIn,
                        gzip = true
                    )
                }
            }
        }
    }

    private fun mkdirAndWriteFile(
        tempDir: File, name: String, isDirectory: Boolean, input: InputStream, gzip: Boolean
    ) {
        val entryFile = File(tempDir, name)
        if (gzip) {
            // 检查并创建父目录
            entryFile.parentFile?.takeIf { !it.exists() }?.mkdirs()
        }
        if (isDirectory) {
            entryFile.mkdirs()
        } else {
            writeFile(entryFile, input)
        }
    }

    private fun writeFile(entryFile: File, input: InputStream) {
        FileOutputStream(entryFile).use { fos ->
            val buffer = ByteArray(1024)
            var len: Int
            while (input.read(buffer).also { len = it } > 0) {
                fos.write(buffer, 0, len)
            }
        }
    }

    fun modifyAndZip(
        tempDir: File,
        projectId: String,
        repoName: String,
        domain: String,
        archiveOutputStream: OutputStream
    ): MutableList<Podspec> {
        // 遍历解压目录中的所有文件，查找 .podspec 或 podspec.json 文件并获取 NAME 和 VERSION
        val podspecList = mutableListOf<Podspec>()

        traverseDirectory(tempDir, podspecList)

        // 修改 podspec 文件
        podspecList.forEach { podspec ->
            val targetPath = podspec.generateCachePath(projectId, repoName, domain)

            modifyPodspecFile(podspec, targetPath)
        }

        // 重新压缩文件，tar.gz
//        GzipCompressorOutputStream(archiveOutputStream).use { gzipOut ->
//            TarArchiveOutputStream(gzipOut).use { tarOut ->
//                val files = tempDir.listFiles()
//                files?.forEach { file ->
//                    addFileToTar(file, tarOut, "")
//                }
//            }
//        }
        compressFilesInBatches(tempDir, archiveOutputStream)
        // 清理临时文件
        deleteDirectory(tempDir)

        return podspecList
    }

    private fun traverseDirectory(dir: File, podspecList: MutableList<Podspec>) {
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                traverseDirectory(file, podspecList)
            } else {
                readFile(file, podspecList)
            }
        }
    }

    private fun readFile(file: File, podspecList: MutableList<Podspec>) {
        if (file.extension != PodSpecType.POD_SPEC.extendedName
            && !file.name.contains(PodSpecType.JSON.extendedName)
        ) return
        // 如果是 .podspec 或 podspec.json 文件，添加到列表

        // 获取上一级文件名 VERSION 和 NAME
        val parentDir = file.parentFile
//                    val version = parentDir.name // 父目录名称就是 VERSION
//                    val name = parentDir.parentFile.name // 父目录的父目录名称就是 NAME
        val podSpecType = (PodSpecType.matchPath(file.path) ?: PodSpecType.POD_SPEC)
        val text = file.readText()
        try {
            val podspec = CocoapodsUtil.parseSourceFromContent(text, podSpecType) ?: return
            podspec.apply {
                this.file = file
                this.fileType = podSpecType
            }
            podspecList.add(podspec)
        } catch (e: Exception) {
            logger.error("parse specs content error: ${e.message}...content:$text")
        }
    }

    @Throws(IOException::class)
    private fun modifyPodspecFile(podspec: Podspec, targetPath: String) {
        val podspecFile = podspec.file!!
        val content = podspecFile.readText()
        val updateContent = when (podspec.fileType!!) {
            PodSpecType.POD_SPEC -> {
                CocoapodsUtil.updatePodspecSource(content, targetPath)
            }

            PodSpecType.JSON -> {
                CocoapodsUtil.updatePodspecJsonSource(content, targetPath)
            }
        }

        // 写回修改后的内容
        podspecFile.writeText(updateContent)
    }

//    @Throws(IOException::class)
//    private fun addFileToZip(file: File, zos: ZipOutputStream, parentDir: String) {
//        if (file.isDirectory) {
//            // 如果是目录，递归添加
//            zos.putNextEntry(ZipEntry(parentDir + file.name + "/"))
//            zos.closeEntry()
//            file.listFiles()?.forEach { subFile ->
//                addFileToZip(subFile, zos, "$parentDir${file.name}/")
//            }
//        } else {
//            // 如果是文件，直接添加
//            zos.putNextEntry(ZipEntry(parentDir + file.name))
//            FileInputStream(file).use { fis ->
//                val buffer = ByteArray(1024)
//                var len: Int
//                while (fis.read(buffer).also { len = it } > 0) {
//                    zos.write(buffer, 0, len)
//                }
//            }
//            zos.closeEntry()
//        }
//    }

//    @Throws(IOException::class)
//    private fun addFileToTar(file: File, tarOut: TarArchiveOutputStream, parentDir: String) {
//        // 设置长路径处理模式为 GNU
//        tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
//        if (file.isDirectory) {
//            // 如果是目录，递归添加
//            tarOut.putArchiveEntry(TarArchiveEntry(file, "$parentDir${file.name}/"))
//            tarOut.closeArchiveEntry()
//            file.listFiles()?.forEach { subFile ->
//                addFileToTar(subFile, tarOut, "$parentDir${file.name}/")
//            }
//        } else {
//            // 如果是文件，直接添加
//            try {
//                tarOut.putArchiveEntry(TarArchiveEntry(file, "$parentDir${file.name}"))
//                FileInputStream(file).use { fis ->
//                    val buffer = ByteArray(1024)
//                    var len: Int
//                    while (fis.read(buffer).also { len = it } > 0) {
//                        tarOut.write(buffer, 0, len)
//                    }
//                }
//                tarOut.closeArchiveEntry()
//            } catch (e: Exception) {
//                logger.warn("Failed to add file to tar: ${e.message}")
//            }
//        }
//    }

    private fun compressFilesInBatches(tempDir: File, archiveOutputStream: OutputStream, batchCount: Int = 10) {
        // 检查是否有 Specs 目录
        val specsDir = File(tempDir, "Specs")
        val files = if (specsDir.exists() && specsDir.isDirectory) {
            specsDir.listFiles()
        } else {
            tempDir.listFiles()
        }

        if (files.isNullOrEmpty()) {
            println("No files to process.")
            return
        }

        // 强制分成 10 个批次
        val totalFiles = files.size
        val batchSize = ceil(totalFiles / batchCount.toDouble()).toInt()

        println("Total files: $totalFiles, Processing in $batchCount batches (batch size: $batchSize)")
        // 处理每一批文件
        TarArchiveOutputStream(BufferedOutputStream(GzipCompressorOutputStream(archiveOutputStream))).use { tarOut ->
//        GzipCompressorOutputStream(archiveOutputStream).use { gzipOut ->
//            TarArchiveOutputStream(gzipOut).use { tarOut ->
            tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU) // 支持长路径

            for (batchIndex in 0 until batchCount) {
                println("Processing batch ${batchIndex + 1} of $batchCount...")
                val startIdx = batchIndex * batchSize
                val endIdx = minOf(startIdx + batchSize, totalFiles)

                // 获取当前批次的文件
                val batchFiles = files.slice(startIdx until endIdx)
                batchFiles.forEach { file ->
                    addFileToTar(file, tarOut, "")
                }
            }
        }
    }

    private fun addFileToTar(file: File, tarOut: TarArchiveOutputStream, parentDir: String) {
        if (file.isDirectory) {
            // 添加目录
            val entry = TarArchiveEntry(file, "$parentDir${file.name}/")
            tarOut.putArchiveEntry(entry)
            tarOut.closeArchiveEntry()

            // 递归处理子文件
            file.listFiles()?.forEach { subFile ->
                addFileToTar(subFile, tarOut, "$parentDir${file.name}/")
            }
        } else {
            addFile(file, tarOut, parentDir)
        }
    }

    private fun addFile(file: File, tarOut: TarArchiveOutputStream, parentDir: String) {
        // 添加文件
        try {
            val entry = TarArchiveEntry(file, "$parentDir${file.name}")
            entry.size = file.length() // 设置文件大小
            tarOut.putArchiveEntry(entry)

            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192) // 使用 8KB 缓冲区
                var len: Int
                while (fis.read(buffer).also { len = it } > 0) {
                    tarOut.write(buffer, 0, len)
                }
            }
            tarOut.closeArchiveEntry()
        } catch (e: Exception) {
            println("Failed to add file to tar: ${e.message}")
        }
    }

    private fun deleteDirectory(dir: File) {
        if (dir.isDirectory) {
            dir.listFiles()?.forEach { file ->
                deleteDirectory(file)
            }
        }
        dir.delete()
    }

    // 假设 Podspec 类
    data class Podspec(
        var name: String,
        var version: String,
        var source: TCocoapodsRemotePackage.Source,
        var file: File? = null,
        var fileType: PodSpecType? = null,
    )

    val logger = LoggerFactory.getLogger(this::class.java)
}

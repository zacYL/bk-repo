package utils

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermission
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream

object FileUtil {

    fun md5(file: File): String {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val fileBytes = Files.readAllBytes(file.toPath())
            digest.update(fileBytes)
            val hashBytes = digest.digest()
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            throw IOException("无法计算文件的MD5值", e)
        }
    }

    fun chmodX(file: File) {
        val permissions = Files.getPosixFilePermissions(file.toPath())
        permissions.add(PosixFilePermission.OWNER_EXECUTE)
        permissions.add(PosixFilePermission.GROUP_EXECUTE)
        permissions.add(PosixFilePermission.OTHERS_EXECUTE)
        Files.setPosixFilePermissions(file.toPath(), permissions)
    }

    fun createDir(first: String, vararg more: String): File {
        return File(first, more.joinToString(File.separator)).apply { mkdirs() }
    }

    fun unzip(source: File, target: File) {
        ZipInputStream(FileInputStream(source)).use { zipIn ->
            unzipChild(zipIn, target)
            zipIn.closeEntry()
        }
    }

    private fun unzipChild(zipIn: ZipInputStream, target: File) {
        var zipEntry = zipIn.nextEntry
        while (zipEntry != null) {
            if (zipEntry.isDirectory) {
                File(target, zipEntry.name).mkdirs()
            } else {
                val file = File(target, zipEntry.name)
                file.outputStream().use { outputStream ->
                    zipIn.copyTo(outputStream)
                }
            }
            zipEntry = zipIn.nextEntry
        }
    }

    fun copyDir(source: java.nio.file.Path, target: java.nio.file.Path) {
        Files.walk(source).forEach { path ->
            val targetPath = target.resolve(source.relativize(path))
            if (Files.isDirectory(path)) {
                Files.createDirectories(targetPath)
            } else if (Files.isSymbolicLink(path)) {
                val linkTarget = Files.readSymbolicLink(path)
                Files.createSymbolicLink(targetPath, linkTarget)
            } else {
                Files.copy(path, targetPath, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    fun compressToTarGz(sourceDir: File, outputFile: File) {
        FileOutputStream(outputFile).use { fos ->
            GzipCompressorOutputStream(fos).use { gzos ->
                TarArchiveOutputStream(gzos).use { tos ->
                    tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
                    addFilesToTarGz(tos, sourceDir, "")
                }
            }
        }
    }

    private fun addFilesToTarGz(tos: TarArchiveOutputStream, file: File, parent: String) {
        val entryName = if (parent.isBlank()) file.name else "$parent/${file.name}"
        val tarEntry: TarArchiveEntry

        if (Files.isSymbolicLink(file.toPath())) {
            val linkTarget = Files.readSymbolicLink(file.toPath()).toString()
            tarEntry = TarArchiveEntry(entryName, TarArchiveEntry.LF_SYMLINK)
            tarEntry.linkName = linkTarget
            tos.putArchiveEntry(tarEntry)
            tos.closeArchiveEntry()
        } else {
            tarEntry = TarArchiveEntry(file, entryName)
            tos.putArchiveEntry(tarEntry)

            if (file.isFile) {
                FileInputStream(file).use { fis ->
                    fis.copyTo(tos)
                }
                tos.closeArchiveEntry()
            } else if (file.isDirectory) {
                tos.closeArchiveEntry()
                for (child in file.listFiles()!!) {
                    addFilesToTarGz(tos, child, entryName)
                }
            }
        }
    }
}
package com.tencent.bkrepo.maven.util

import com.tencent.bkrepo.common.api.util.DecompressUtils
import com.tencent.bkrepo.maven.exception.JarFormatException
import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.codehaus.plexus.util.xml.pull.XmlPullParserException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.jar.JarFile

object JarUtils {

    private val logger: Logger = LoggerFactory.getLogger(JarUtils::class.java)

    private val noPom = ByteArray(0)

    fun parseModelInJar(jarFile: File): Model {
        try {
            JarFile(jarFile)
        } catch (e: IOException) {
            throw JarFormatException("only jar file is supported")
        }
        val model = DecompressUtils.doWithArchiver<Model, Model>(
            jarFile.inputStream(),
            callbackPre = { it.name.endsWith("pom.xml") },
            callback = { stream, _ -> readModel(stream).transform() },
            handleResult = { _, e, _ -> e },
            callbackPost = { _, e -> e != null }
        )
        return model ?: throw JarFormatException("pom.xml not found")
    }

    fun readModel(inputStream: InputStream): Model {
        return try {
            MavenXpp3Reader().read(inputStream).transform()
        } catch (e: IOException) {
            logger.error("Error reading POM ", e)
            throw JarFormatException("")
        } catch (e: XmlPullParserException) {
            logger.error("Error parsing POM ", e)
            throw JarFormatException("")
        }
    }

    /**
     * 解析模型从给定的输入流中。
     * 此函数首先从输入流中提取pom.xml文件内容，然后根据内容创建模型对象。
     * 如果pom.xml内容为空，则返回一个空的模型对象。
     *
     * @param inputStream 包含项目信息的输入流，通常是一个压缩文件流。
     * @return 解析后的模型对象。
     */
    fun parseModel(inputStream: InputStream): Model {
        val pom = extractPom(inputStream)
        return if (isEmptyPom(pom)) Model() else readModel(pom.inputStream())
    }

    /**
     * 从输入流中提取pom.xml文件的字节内容。
     * 此函数使用DecompressUtils工具类来解压缩输入流，并查找名为pom.xml的文件。
     * 如果找到，则读取其内容为字节数组；如果没有找到，则返回一个预定义的空字节数组。
     *
     * @param inputStream 包含项目信息的输入流，通常是一个压缩文件流。
     * @return pom.xml文件的字节内容，如果没有找到则返回空字节数组。
     */
    private fun extractPom(inputStream: InputStream): ByteArray {
        val bytes = DecompressUtils.doWithArchiver<ByteArray, ByteArray>(
            inputStream,
            callbackPre = { it.name.endsWith("pom.xml") },
            callback = { stream, _ -> stream.readBytes() },
            handleResult = { _, e, _ -> e },
            callbackPost = { _, e -> e != null }
        )
        return bytes ?: noPom
    }

    /**
     * 检查给定的pom.xml内容是否为空。
     * 此函数通过比较给定的字节数组是否等于预定义的空字节数组来判断pom.xml内容是否为空。
     *
     * @param pom pom.xml文件的字节内容。
     * @return 如果pom.xml内容为空则返回true，否则返回false。
     */
    private fun isEmptyPom(pom: ByteArray) = pom === noPom

    private fun Model.transform(): Model {
        val parent = this.parent
        with(this) {
            if (groupId == null) groupId = parent?.groupId
            if (version == null) version = parent?.version
            if (packaging == "bundle") packaging = "jar"
        }
        return this
    }

}

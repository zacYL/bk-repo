package com.tencent.bkrepo.maven.util

import com.tencent.bkrepo.maven.exception.JarFormatException
import com.tencent.bkrepo.maven.pojo.request.MavenWebDeployRequest
import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.codehaus.plexus.util.IOUtil
import org.codehaus.plexus.util.ReaderFactory
import org.codehaus.plexus.util.xml.pull.XmlPullParserException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.jar.JarFile
import java.util.regex.Pattern

object JarUtils {

    private val logger: Logger = LoggerFactory.getLogger(JarUtils::class.java)

    // pom.xml 文件位置
    private var pomEntry: Pattern = Pattern.compile("META-INF/maven/.*/pom\\.xml")

    // find pom.xml in jar
    fun parsePomInJar(jarFile: File): Pair<File, Model> {
        val jar = JarFile(jarFile)
        val entries = jar.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            if (pomEntry.matcher(entry.name).matches()) {
                jar.getInputStream(entry).use {
                    var filename = jarFile.name
                    if (filename.indexOf('.') > 0) {
                        filename = filename.substring(0, filename.lastIndexOf('.'))
                    }
                    val pomFile = File(jarFile.parentFile, "$filename.pom")
                    FileOutputStream(pomFile).use { pomOutputStream ->
                        IOUtil.copy(it, pomOutputStream)
                        return Pair(pomFile, readModel(pomFile).apply { processModel(this) })
                    }
                }
            }
        }
        throw JarFormatException("pom.xml not found")
    }

    fun parseModelInJar(jarFile: File): Model {
        val jar = try {
            JarFile(jarFile)
        } catch (e: IOException) {
            throw JarFormatException("only jar file is supported")
        }
        val entries = jar.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            if (pomEntry.matcher(entry.name).matches()) {
                jar.getInputStream(entry).use {
                    return readModel(it).apply { processModel(this) }
                }
            }
        }
        throw JarFormatException("pom.xml not found")
    }

    fun readModel(pomFile: File): Model {
        return try {
            FileInputStream(pomFile).use {
                readModel(it)
            }
        } catch (e: FileNotFoundException) {
            logger.error("pom file not found: ${pomFile.absolutePath}", e)
            throw JarFormatException("")
        } catch (e: IOException) {
            logger.error("Error reading POM file: : ${pomFile.absolutePath}", e)
            throw JarFormatException("")
        }
    }

    fun readModel(inputStream: InputStream): Model {
        return try {
            ReaderFactory.newXmlReader(inputStream).use {
                MavenXpp3Reader().read(it)
            }.apply { processModel(this) }
        } catch (e: IOException) {
            logger.error("Error reading POM ", e)
            throw JarFormatException("")
        } catch (e: XmlPullParserException) {
            logger.error("Error parsing POM ", e)
            throw JarFormatException("")
        }
    }

    private fun processModel(model: Model) {
        val parent = model.parent
        with(model) {
            if (groupId == null) groupId = parent?.groupId
            if (version == null) version = parent?.version
            if (packaging == "bundle") packaging = "jar"
        }
    }

    /**
     * 按照请求参数构建model
     */
    fun processModel(model: Model, request: MavenWebDeployRequest) {
        with(request) {
            if (groupId != null && groupId!!.isNotBlank() && request.groupId != model.groupId) {
                model.groupId = request.groupId
            }
            if (artifactId != null && artifactId!!.isNotBlank() && request.artifactId != model.artifactId) {
                model.artifactId = request.artifactId
            }
            if (version != null && version!!.isNotBlank() && request.version != model.version) {
                model.version = request.version
            }
            if (type != null && type!!.isNotBlank() && request.type != model.packaging) {
                if (model.packaging == "pom") return@with
                model.packaging = request.type
            }
        }
    }
}

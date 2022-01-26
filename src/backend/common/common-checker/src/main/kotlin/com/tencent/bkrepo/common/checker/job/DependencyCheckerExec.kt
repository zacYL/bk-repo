package com.tencent.bkrepo.common.checker.job

import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.checker.pojo.DependencyInfo
import org.owasp.dependencycheck.StartApp

object DependencyCheckerExec {

    fun run(path: String): HashMap<String, Any> {
        return StartApp.startRun(path, false)
    }
}

fun main() {
    val map = DependencyCheckerExec.run("/Users/weaving/Downloads/scanTest")
    val dependencyInfo = (map["report"] as? String)?.readJsonString<DependencyInfo>()
    println("$dependencyInfo")
}

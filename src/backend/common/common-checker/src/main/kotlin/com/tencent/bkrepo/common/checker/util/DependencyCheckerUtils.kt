package com.tencent.bkrepo.common.checker.util

import org.owasp.dependencycheck.StartApp

object DependencyCheckerUtils {
    fun run(path: String): HashMap<String, Any> {
        return StartApp.startRun(path, false)
    }
}

package com.tencent.bkrepo.scanner.service

import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.checker.pojo.DependencyInfo
import com.tencent.bkrepo.common.checker.util.DependencyCheckerUtils

fun main() {
    val map = DependencyCheckerUtils.run("/Users/weaving/IdeaProjects2/artifact/data/store/f1/a5/f1a5e1a3475918d610791ccb61ed5f93385cc12b00aa9d83d3a800e144e1795b")
    val dependencyInfo = (map["report"] as? String)?.readJsonString<DependencyInfo>()
    println("$dependencyInfo")
}

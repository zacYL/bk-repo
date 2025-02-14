package com.tencent.bkrepo.cocoapods.pojo.enums

/**
 * .podspec文件类型
 */
enum class PodSpecType(val extendedName:String) {
    POD_SPEC("podspec"), JSON("podspec.json");
    companion object {
        fun extendedNames(): List<String> {
            return values().map { it.extendedName }
        }

        fun matchPath(url: String): PodSpecType? {
            return values().firstOrNull { url.endsWith(it.extendedName) }
        }
    }
}

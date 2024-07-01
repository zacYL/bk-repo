package com.tencent.bkrepo.conan.pojo.enums

import com.tencent.bkrepo.conan.pojo.artifact.ConanArtifactInfo.Companion.PACKAGE_SEARCH_V2
import com.tencent.bkrepo.conan.pojo.artifact.ConanArtifactInfo.Companion.RECIPE_LATEST_V2
import com.tencent.bkrepo.conan.pojo.artifact.ConanArtifactInfo.Companion.SEARCH_V2

enum class ConanRequestType(private val path: String) {
    SEARCH(SEARCH_V2),RECIPE_SEARCH(PACKAGE_SEARCH_V2), RECIPE_LATEST(RECIPE_LATEST_V2);

    companion object{
        fun matchPath(url: String): ConanRequestType? {
            return ConanRequestType.values().firstOrNull {
                val regexPattern = it.path.replace(".", "\\.")
                    .replace("{", "(?<")
                    .replace("}", ">[^/]+)")
                    .replace("/", "\\/")
                val regex = "^$regexPattern$".toRegex()
                regex.matches(url)
            }
        }
    }
}


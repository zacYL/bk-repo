package com.tencent.bkrepo.common.artifact.interceptor.impl

import com.tencent.bkrepo.common.artifact.exception.PathPatternNotMatchException
import com.tencent.bkrepo.common.artifact.interceptor.DownloadInterceptor
import org.springframework.util.AntPathMatcher

/**
 * 路径模式下载拦截器
 */
class PathPatternInterceptor(rules: Map<String, Any>)
    : DownloadInterceptor<Pair<List<String>, List<String>>, String>(rules) {

    @Suppress("UNCHECKED_CAST")
    override fun parseRule(): Pair<List<String>, List<String>> {
        val includes = rules[INCLUDE_PATH_PATTERNS] as? List<String> ?: emptyList()
        val excludes = rules[EXCLUDE_PATH_PATTERNS] as? List<String> ?: emptyList()
        return Pair(includes.filter { it.isNotBlank() }, excludes.filter { it.isNotBlank() })
    }

    override fun matcher(artifact: String, rule: Pair<List<String>, List<String>>): Boolean {
        val path = artifact.removePrefix("/")
        val (includes, excludes) = rule
        return (includes.isEmpty() || includes.any { antPathMatcher.match(it.removePrefix("/"), path) }) &&
                (excludes.isEmpty() || excludes.none { antPathMatcher.match(it.removePrefix("/"), path) })
    }

    override fun constructException(projectId: String, artifact: String): Exception {
        return PathPatternNotMatchException(artifact)
    }

    companion object {
        private val antPathMatcher = AntPathMatcher()
        private const val INCLUDE_PATH_PATTERNS = "includePathPatterns"
        private const val EXCLUDE_PATH_PATTERNS = "excludePathPatterns"
    }
}

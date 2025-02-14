package com.tencent.bkrepo.common.metadata.interceptor.impl

import com.tencent.bkrepo.common.artifact.event.base.EventType
import com.tencent.bkrepo.common.artifact.exception.PathPatternNotMatchException
import com.tencent.bkrepo.common.metadata.interceptor.DownloadInterceptor
import com.tencent.bkrepo.common.metadata.model.TOperateLog.Companion.DESCRIPTION_KEY_FAIL_REASON
import com.tencent.bkrepo.common.metadata.pojo.log.OperateLog
import com.tencent.bkrepo.common.metadata.service.log.OperateLogService
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.common.service.util.SpringContextUtils
import org.springframework.util.AntPathMatcher
import java.time.LocalDateTime

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

    override fun onForbidden(projectId: String, artifact: String, rule: Pair<List<String>, List<String>>) {
        val log = OperateLog(
            createdDate = LocalDateTime.now(),
            type = EventType.NODE_DOWNLOADED.name,
            projectId = projectId,
            repoName = null,
            resourceKey = artifact,
            userId = SecurityUtils.getUserId(),
            clientAddress = HttpContextHolder.getClientAddress(),
            description = mapOf(
                DESCRIPTION_KEY_FAIL_REASON to PATH_PATTERN_NOT_MATCHED,
                INCLUDE_PATH_PATTERNS to rule.first,
                EXCLUDE_PATH_PATTERNS to rule.second
            ),
            result = false
        )
        SpringContextUtils.getBean(OperateLogService::class.java).saveAsync(log)
    }

    companion object {
        private val antPathMatcher = AntPathMatcher()
        private const val INCLUDE_PATH_PATTERNS = "includePathPatterns"
        private const val EXCLUDE_PATH_PATTERNS = "excludePathPatterns"
        private const val PATH_PATTERN_NOT_MATCHED = "Artifact not match path-pattern settings of repository"
    }
}

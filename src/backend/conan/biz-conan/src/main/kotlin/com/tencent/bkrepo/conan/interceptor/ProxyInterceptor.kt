package com.tencent.bkrepo.conan.interceptor

import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.conan.service.ConanRemoteService
import org.springframework.web.servlet.HandlerInterceptor
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class ProxyInterceptor(private val conanRemoteService: ConanRemoteService) : HandlerInterceptor {
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        with(ArtifactContextHolder.getRepoDetail()!!) {
            if (category == RepositoryCategory.REMOTE && request.method == "GET") {
                val regex = "/\\w+/files/[^/]+".toRegex()
                return if (regex.containsMatchIn(request.requestURI)) {
                    //下载请求不拦截
                    true
                } else {
                    conanRemoteService.proxyRequestToRemoteUrl(this, response)
                    //停止继续处理请求
                    false
                }
            }
        }
        return true
    }
}

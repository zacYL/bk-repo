package com.tencent.bkrepo.common.security.interceptor.anonymous

import com.tencent.bkrepo.common.api.constant.ANONYMOUS_USER
import com.tencent.bkrepo.common.api.constant.HttpHeaders.X_FORWARDED_FOR
import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.api.util.IpUtils
import com.tencent.bkrepo.common.security.exception.AuthenticationException
import com.tencent.bkrepo.common.security.util.SecurityUtils
import org.slf4j.LoggerFactory
import org.springframework.web.servlet.HandlerInterceptor
import sun.net.util.IPAddressUtil
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class AnonymousAccessInterceptor(private val properties: AnonymousInterceptorProperties) : HandlerInterceptor {

    private val blackList: List<String>
    private val whiteList: List<String>

    init {
        blackList = filterValidCidr(properties.cidr.blackList)
        whiteList = filterValidCidr(properties.cidr.whiteList)
    }

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        if (!properties.enabled || SecurityUtils.getUserId() != ANONYMOUS_USER) {
            return true
        }
        val xForwardedFor = request.getHeader(X_FORWARDED_FOR)?.split(StringPool.COMMA)?.map { it.trim() }
            ?.filter { IPAddressUtil.isIPv4LiteralAddress(it) }
        return isAllowed(xForwardedFor) || throw AuthenticationException()
    }

    private fun filterValidCidr(list: List<String>) = list.filter {
        try {
            IpUtils.parseCidr(it)
            true
        } catch (ignore: IllegalArgumentException) {
            logger.warn("[$it] is not a valid CIDR")
            false
        }
    }

    private fun isAllowed(ipList: List<String>?) =
        if (properties.cidr.blackList.isNotEmpty()) {
            ipList?.none { ip -> blackList.any { IpUtils.isInRange(ip, it) } } ?: true
        } else if (properties.cidr.whiteList.isNotEmpty()) {
            ipList?.any { ip -> whiteList.any { IpUtils.isInRange(ip, it) } } ?: false
        } else {
            true
        }

    companion object {
        private val logger = LoggerFactory.getLogger(AnonymousAccessInterceptor::class.java)
    }
}

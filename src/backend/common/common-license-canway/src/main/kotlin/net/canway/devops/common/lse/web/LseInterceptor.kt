package net.canway.devops.common.lse.web

import net.canway.devops.common.lse.LseChecker
import org.apache.http.HttpStatus
import org.slf4j.LoggerFactory
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class LseInterceptor constructor(
    private val lseChecker: LseChecker
) : HandlerInterceptorAdapter() {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        return try {
            lseChecker.checkLse()
            true
        } catch (e: Exception) {
            logger.warn("check license fail ${e.message}")
            response.status = HttpStatus.SC_BAD_REQUEST
            response.writer.print(e.message)
            false
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LseInterceptor::class.java)
    }
}

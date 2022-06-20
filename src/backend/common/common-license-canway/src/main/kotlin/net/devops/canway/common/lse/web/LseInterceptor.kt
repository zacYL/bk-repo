package net.devops.canway.common.lse.web

import com.tencent.bkrepo.common.api.constant.HttpStatus
import io.undertow.util.BadRequestException
import net.canway.license.utils.Constant
import net.devops.canway.common.lse.LseChecker
import org.slf4j.LoggerFactory
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class LseInterceptor constructor(
    private val lseChecker: LseChecker
) : HandlerInterceptorAdapter() {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        try {
            lseChecker.checkLse().let {
                return if (it.code == Constant.SUCCESS)
                    true
                else
                    throw BadRequestException("${it.message}:${it.code}")
            }
        } catch (e: Exception) {
            logger.warn("check license fail ${e.message}")
            response.status = HttpStatus.BAD_REQUEST.value
            response.writer.print(e.message)
            return false
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LseInterceptor::class.java)
    }
}

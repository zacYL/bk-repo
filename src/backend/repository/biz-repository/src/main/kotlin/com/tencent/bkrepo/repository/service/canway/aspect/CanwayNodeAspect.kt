package com.tencent.bkrepo.repository.service.canway.aspect

import com.tencent.bkrepo.common.api.constant.USER_KEY
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.devops.exception.CanwayPermissionException
import com.tencent.bkrepo.common.security.exception.PermissionException
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.repository.BK_SOFTWARE
import com.tencent.bkrepo.repository.service.canway.ACCESS
import com.tencent.bkrepo.repository.service.canway.BKTOKEN
import com.tencent.bkrepo.repository.service.canway.CANWAY_PERMISSION
import com.tencent.bkrepo.repository.service.canway.service.CanwayPermissionService
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.beans.factory.annotation.Autowired

@Aspect
class CanwayNodeAspect {

    @Autowired
    lateinit var canwayPermissionService: CanwayPermissionService

    @Around(value = "execution(* com.tencent.bkrepo.repository.service.node.impl.NodeServiceImpl.listNodePage(..))")
    fun beforeNodePage(point: ProceedingJoinPoint): Any {
        val args = point.args
        val artifactInfo = args.first() as ArtifactInfo
        return if (artifactInfo.projectId == BK_SOFTWARE) {
            point.proceed(args)
        } else {
            val request = HttpContextHolder.getRequest()
            val api = request.requestURI.removePrefix("/").removePrefix("web/")
            if (api.startsWith("api", ignoreCase = true)) {
                val userId = request.getAttribute(USER_KEY) ?: throw PermissionException()
                if (!canwayPermissionService.checkCanwayPermission(
                        artifactInfo.projectId, artifactInfo.repoName, userId as String, ACCESS
                    )
                )
                    throw CanwayPermissionException(CANWAY_PERMISSION)
            }
            point.proceed(args)
        }

    }
}

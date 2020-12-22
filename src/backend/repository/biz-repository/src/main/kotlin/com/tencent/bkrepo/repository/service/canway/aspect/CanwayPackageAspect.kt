package com.tencent.bkrepo.repository.service.canway.aspect

import com.tencent.bkrepo.common.security.exception.AccessDeniedException
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.repository.service.canway.ACCESS
import com.tencent.bkrepo.repository.service.canway.bk.BkUserService
import com.tencent.bkrepo.repository.service.canway.exception.CanwayPermissionException
import com.tencent.bkrepo.repository.service.canway.service.CanwayPermissionService
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Aspect
@Component
class CanwayPackageAspect(
    val bkUserService: BkUserService
) {

    @Autowired
    lateinit var canwayPermissionService: CanwayPermissionService

    @Around(value = "execution(* com.tencent.bkrepo.repository.service.impl.PackageServiceImpl.listPackagePage(..))")
    fun beforePackagePage(point: ProceedingJoinPoint): Any {
        val args = point.args
        val projectId = args.first() as String
        val repoName = args[1] as String
        val request = HttpContextHolder.getRequest()
        val api = request.requestURI.removePrefix("/").removePrefix("web/")
        if (api.startsWith("api", ignoreCase = true)) {
            val userId = bkUserService.getBkUser()
            if (!canwayPermissionService.checkCanwayPermission(projectId, repoName, userId, ACCESS))
                throw CanwayPermissionException()
        }
        return point.proceed(args)
    }
}

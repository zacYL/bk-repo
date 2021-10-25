package com.tencent.bkrepo.common.devops.repository.permission

import com.tencent.bkrepo.auth.constant.BK_SOFTWARE
import com.tencent.bkrepo.common.api.constant.USER_KEY
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.devops.CANWAY_PERMISSION
import com.tencent.bkrepo.common.devops.api.exception.CanwayPermissionException
import com.tencent.bkrepo.common.devops.repository.service.CanwayPermissionService
import com.tencent.bkrepo.common.security.exception.PermissionException
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.stereotype.Component

@Aspect
@Component
class CanwayPermissionAspect(
    private val canwayPermissionService: CanwayPermissionService
) {
    @Around("@annotation(com.tencent.bkrepo.common.devops.repository.permission.CanwayPermission)")
    fun around(point: ProceedingJoinPoint): Any? {
        val repoDetail = ArtifactContextHolder.getRepoDetail()!!
        if (repoDetail.projectId != BK_SOFTWARE) {
            val request = HttpContextHolder.getRequest()
            val userId = request.getAttribute(USER_KEY) ?: throw PermissionException()
            val signature = point.signature as MethodSignature
            val canwayPermission = signature.method.getAnnotation(CanwayPermission::class.java)
            val api = request.requestURI.removePrefix("/").removePrefix("web/")
            if (api.startsWith("api", ignoreCase = true)) {
                if (!canwayPermissionService.checkCanwayPermission(
                    repoDetail.projectId, repoDetail.name, userId as String, canwayPermission.type
                )
                ) throw CanwayPermissionException(CANWAY_PERMISSION)
            }
        }
        return point.proceed()
    }
}

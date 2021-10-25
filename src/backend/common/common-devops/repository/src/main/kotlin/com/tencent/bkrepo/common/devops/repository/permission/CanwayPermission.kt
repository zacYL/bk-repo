package com.tencent.bkrepo.common.devops.repository.permission

import com.tencent.bkrepo.common.devops.api.enums.CanwayPermissionType
import java.lang.annotation.Inherited

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@Inherited
@MustBeDocumented
annotation class CanwayPermission(
    val type: CanwayPermissionType
)

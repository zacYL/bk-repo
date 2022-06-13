package net.canway.devops.common.lse.feign

import com.tencent.bkrepo.common.api.constant.LICENSE_SERVICE_NAME
import net.canway.license.api.LicenseApi
import org.springframework.cloud.openfeign.FeignClient

@FeignClient(
    LICENSE_SERVICE_NAME,
    path = "/api",
    configuration = [FeignConfiguration::class],
    contextId = "LicenseFeign"
)
interface LicenseFeign : LicenseApi

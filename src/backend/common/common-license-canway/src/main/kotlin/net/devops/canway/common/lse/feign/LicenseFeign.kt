package net.devops.canway.common.lse.feign

import com.tencent.bkrepo.common.api.constant.LICENSE_SERVICE_NAME
import net.canway.license.api.LicenseApi
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.stereotype.Service

@Service
@FeignClient(LICENSE_SERVICE_NAME, configuration = [FeignConfiguration::class], contextId = "LicenseFeign")
interface LicenseFeign : LicenseApi

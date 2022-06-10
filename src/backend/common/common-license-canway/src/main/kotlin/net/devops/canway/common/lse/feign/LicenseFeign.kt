package net.devops.canway.common.lse.feign

import net.canway.license.api.LicenseApi
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.stereotype.Service

@Service
@FeignClient("license-devops", configuration = [FeignConfiguration::class], contextId = "LicenseFeign")
interface LicenseFeign : LicenseApi

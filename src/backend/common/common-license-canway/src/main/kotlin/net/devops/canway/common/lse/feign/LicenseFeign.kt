package net.devops.canway.common.lse.feign

import net.canway.license.api.LicenseApi
import org.springframework.cloud.openfeign.FeignClient

@FeignClient("license\${service-suffix:#{null}}")
interface LicenseFeign : LicenseApi

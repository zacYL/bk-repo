package net.canway.devops.common.lse.service

import net.canway.devops.common.lse.pojo.LicenseInfo

interface LicenseService {
    fun getLicense(): LicenseInfo
}

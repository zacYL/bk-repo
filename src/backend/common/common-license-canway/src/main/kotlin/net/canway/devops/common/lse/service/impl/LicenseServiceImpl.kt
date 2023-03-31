package net.canway.devops.common.lse.service.impl

import net.canway.devops.common.lse.LseChecker
import net.canway.devops.common.lse.pojo.LicenseInfo
import net.canway.devops.common.lse.service.LicenseService
import org.springframework.stereotype.Service

@Service
class LicenseServiceImpl(
    private val lseChecker: LseChecker
) : LicenseService {
    override fun getLicense(): LicenseInfo {
        with(lseChecker.checkLse()) {
            return LicenseInfo(
                productKey,
                productName,
                versionType,
                modules,
                applyUserNumber,
                applyNodeNumber,
                validityTime,
                subscribeType
            )
        }
    }
}

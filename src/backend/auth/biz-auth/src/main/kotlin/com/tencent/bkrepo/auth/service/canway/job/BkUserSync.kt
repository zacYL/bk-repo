package com.tencent.bkrepo.auth.service.canway.job

import com.tencent.bkrepo.auth.service.UserService
import com.tencent.bkrepo.auth.service.canway.bk.BkUserService
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class BkUserSync(
    val bkUserService: BkUserService
) {
    @Autowired
    lateinit var userService: UserService

    @Scheduled(fixedDelay = 3600 * 1000)
    @SchedulerLock(name = "BkUserSync", lockAtMostFor = "PT60M")
    fun syncBkUser() {
        bkUserService.syncBkUser()
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(BkUserSync::class.java)
    }
}

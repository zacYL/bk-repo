package com.tencent.bkrepo.auth.service.canway.job

import com.tencent.bkrepo.auth.service.canway.bk.BkUserService
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.util.StopWatch

@Component
class BkUserSync(
    val bkUserService: BkUserService
) {

    @Scheduled(fixedDelay = 600 * 1000)
    @SchedulerLock(name = "BkUserSync", lockAtMostFor = "PT60M")
    fun syncBkUser() {
        val sw = StopWatch()
        logger.info("Start sync bk users")
        sw.start("sync bk users")
        bkUserService.syncBkUser()
        sw.stop()
        logger.info("Sync bk users cost time: ${sw.totalTimeMillis}")
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(BkUserSync::class.java)
    }
}

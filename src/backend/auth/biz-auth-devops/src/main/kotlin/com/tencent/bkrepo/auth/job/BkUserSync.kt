package com.tencent.bkrepo.auth.job

import com.tencent.bkrepo.auth.constant.DEFAULT_PASSWORD
import com.tencent.bkrepo.common.devops.api.pojo.BkPage
import com.tencent.bkrepo.common.devops.api.pojo.BkUser
import com.tencent.bkrepo.auth.pojo.user.CreateUserRequest
import com.tencent.bkrepo.auth.pojo.user.UpdateUserRequest
import com.tencent.bkrepo.auth.pojo.user.User
import com.tencent.bkrepo.auth.service.UserService
import com.tencent.bkrepo.common.devops.api.service.BkUserService
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.util.StopWatch

@Component
class BkUserSync(
    private val bkUserService: BkUserService,
    private val userService: UserService
) {

    @Scheduled(fixedDelay = 300 * 1000, initialDelay = 300 * 1000)
    @SchedulerLock(name = "BkUserSync", lockAtMostFor = "PT60M")
    fun execute() {
        val sw = StopWatch()
        logger.info("Start sync bk users")
        sw.start("sync bk users")
        syncBkUser()
        sw.stop()
        logger.info("Sync bk users cost time: ${sw.totalTimeMillis}")
    }

    fun syncBkUser() {
        val count = bkUserService.getBkUsers(1, 1)?.count ?: return
        val pages = (count / pageSize).inc()
        for (i in 1..pages) {
            val userPage = bkUserService.getBkUsers(i, pageSize)
            checkUserExist(userPage)
        }
    }

    private fun checkUserExist(bkUsers: BkPage<BkUser>?) {
        val userList = bkUsers?.results ?: return
        for (bkUser in userList) {
            val bkrepoUser = userService.getUserById(bkUser.username)
            if (bkrepoUser == null) {
                val createUserRequest = CreateUserRequest(
                    userId = bkUser.username,
                    name = bkUser.displayName,
                    pwd = DEFAULT_PASSWORD,
                    admin = false
                )
                userService.createUser(createUserRequest)
            } else {
                checkUserName(bkrepoUser, bkUser)
            }
        }
    }

    private fun checkUserName(bkrepoUser: User, bkUser: BkUser) {
        if (bkrepoUser.name != bkUser.displayName) {
            userService.updateUserById(
                bkrepoUser.userId,
                UpdateUserRequest(
                    name = bkUser.displayName
                )
            )
            BkUserService.logger.info("${bkrepoUser.userId} name : ${bkrepoUser.name} to ${bkUser.displayName}")
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(BkUserSync::class.java)
        const val pageSize = 50
    }
}

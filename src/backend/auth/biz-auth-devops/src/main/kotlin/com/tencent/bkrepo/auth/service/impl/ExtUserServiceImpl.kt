package com.tencent.bkrepo.auth.service.impl

import com.tencent.bkrepo.auth.api.CanwayUsermangerClient
import com.tencent.bkrepo.auth.constant.AUTH_ADMIN
import com.tencent.bkrepo.auth.pojo.user.UserInsertVO
import com.tencent.bkrepo.auth.pojo.user.UserPasswordUpdateVO
import com.tencent.bkrepo.auth.pojo.user.UserRequest
import com.tencent.bkrepo.auth.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ExtUserServiceImpl(
    private val userService: UserService,
    private val canwayUsermangerClient: CanwayUsermangerClient
) {
    /**
     * 独立制品库升级至集成CI方式，迁移用户方法；
     */
    @Suppress("TooGenericExceptionCaught")
    fun migrateUserToDevOps() {
        // 获取独立制品库所有用户信息
        val userList = userService.userAll(null, null, null)
        // 查询集成环境所有用户信息
        val canwayUserList = canwayUsermangerClient.getAllUser(false, null).data
        val canwayUserIdList = canwayUserList?.map { it.id }
        if (canwayUserIdList.isNullOrEmpty()) return
        // 创建手机号码（防止手机为空情况）
        var telephone = generateRandomPhoneNumber()
        // 创建邮箱（防止邮箱为空情况）
        var emailNumber = 0
        userList.forEach { user ->
            if (user.userId !in canwayUserIdList) {
                val cwTelephone: String = user.phone.takeIf { !it.isNullOrBlank() } ?: run {
                    telephone = (telephone.toLong() + 1).toString()
                    telephone
                }
                // 创建用户
                canwayUsermangerClient.addUsers(
                    loginUserId = AUTH_ADMIN,
                    UserRequest(
                        userInsertVO = UserInsertVO(
                            userId = user.userId,
                            displayName = user.name,
                            headPortrait = "",
                            email = if (user.email.isNullOrBlank()) generateRandomEmail(emailNumber++)
                            else user.email!!,
                            telephone = cwTelephone
                        ),
                        orgIds = listOf("1")
                    )
                )
                if (user.phone.isNullOrBlank()) {
                    telephone = (telephone.toLong() + 1).toString()
                }
                // 修改密码
                canwayUsermangerClient.passwordUpdate(
                    loginUserId = AUTH_ADMIN,
                    userPasswordUpdateVO = UserPasswordUpdateVO(
                        username = user.userId,
                        password = user.pwd,
                        isCpackUser = true
                    )
                )
                logger.info("[${user.userId}] user completed migration")
            }
        }
    }

    private val logger = LoggerFactory.getLogger(ExtUserServiceImpl::class.java)

    fun generateRandomPhoneNumber(): String {
        val sb = StringBuilder()
        val random = java.util.Random()
        sb.append("13") // 手机号码以1开头
        repeat(9) {
            sb.append(random.nextInt(9)) // 生成随机数字
        }
        return sb.toString()
    }

    fun generateRandomEmail(number: Int): String {
        val chars = ('a'..'z') + ('0'..'9') // 定义字符集
        val sb = StringBuilder()
        val random = java.util.Random()
        repeat(10) {
            val index = random.nextInt(chars.size)
            sb.append(chars[index])
        }
        sb.append(number.toString())
        sb.append("@example.com")
        return sb.toString()
    }
}

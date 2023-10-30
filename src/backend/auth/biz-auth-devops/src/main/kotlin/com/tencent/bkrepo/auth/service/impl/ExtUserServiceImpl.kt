package com.tencent.bkrepo.auth.service.impl

import com.tencent.bkrepo.auth.api.CanwayUsermangerClient
import com.tencent.bkrepo.auth.constant.AUTH_ADMIN
import com.tencent.bkrepo.auth.pojo.user.UserInsertVO
import com.tencent.bkrepo.auth.pojo.user.UserPasswordUpdateVO
import com.tencent.bkrepo.auth.pojo.user.UserRequest
import com.tencent.bkrepo.auth.service.UserService
import org.slf4j.LoggerFactory

@Suppress("TYPE_INFERENCE_ONLY_INPUT_TYPES_WARNING")
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
        userList.forEach { user ->
            with(user) {
                if (canwayUserIdList != null) {
                    if (user !in canwayUserIdList) {
                        // 创建用户
                        canwayUsermangerClient.addUsers(
                            loginUserId = AUTH_ADMIN,
                            UserRequest(
                                UserInsertVO(
                                    userId = userId,
                                    displayName = name,
                                    headPortrait = "",
                                    email = email ?: "",
                                    telephone = phone ?: ""
                                )
                            )
                        )
                        // 修改密码
                        canwayUsermangerClient.passwordUpdate(
                            loginUserId = AUTH_ADMIN,
                            userPasswordUpdateVO = UserPasswordUpdateVO(
                                username = userId,
                                password = pwd,
                                isCpackUser = true
                            )
                        )
                        logger.info("[$userId] user completed migration")
                    }
                }
            }
        }
    }

    private val logger = LoggerFactory.getLogger(ExtUserServiceImpl::class.java)
}

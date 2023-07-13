package com.tencent.bkrepo.auth.service.impl


import com.tencent.bkrepo.auth.api.CanwayUsermangerClient
import com.tencent.bkrepo.auth.pojo.MigrateTokenVO
import com.tencent.bkrepo.auth.service.UserService
import com.tencent.bkrepo.common.api.constant.StringPool
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ExtTokenServiceImpl(
    private val userService: UserService,
    private val canwayUsermangerClient: CanwayUsermangerClient
) {
    @Suppress("TooGenericExceptionCaught")
    fun migHistoryTokenData() {
        //查询所有用户信息
        val userList = userService.userAll(null, null, null)
        val migrateTokenList = mutableListOf<MigrateTokenVO>()
        logger.info("Migrate User ${userList} token")
        userList.forEach { user ->
            if (user.tokens.isNotEmpty()) {
                migrateTokenList.addAll(
                    user.tokens.map { token ->
                        MigrateTokenVO(
                            userId = user.userId,
                            tokenName = token.name ?: StringPool.EMPTY,
                            token = token.id
                        )
                    })
            }
        }
        logger.info("Migrate User token list: ${migrateTokenList}")
        canwayUsermangerClient.migrateToken(migrateTokenList)
    }

    private val logger = LoggerFactory.getLogger(ExtTokenServiceImpl::class.java)
}
package com.tencent.bkrepo.auth.service.canway

import com.tencent.bkrepo.auth.pojo.user.UserResult
import com.tencent.bkrepo.auth.repository.RoleRepository
import com.tencent.bkrepo.auth.repository.UserRepository
import com.tencent.bkrepo.auth.service.canway.conf.CanwayAuthConf
import com.tencent.bkrepo.auth.service.canway.pojo.CanwayResponse
import com.tencent.bkrepo.auth.service.canway.pojo.CanwayUser
import com.tencent.bkrepo.auth.service.local.UserServiceImpl
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.devops.http.CanwayHttpUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate

class CanwayUserServiceImpl(
    userRepository: UserRepository,
    roleRepository: RoleRepository,
    mongoTemplate: MongoTemplate,
    private val canwayAuthConf: CanwayAuthConf
) : UserServiceImpl(userRepository, roleRepository, mongoTemplate) {

    @Suppress("TooGenericExceptionCaught")
    override fun listUserResult(rids: List<String>): List<UserResult> {
        val devopsHost = canwayAuthConf.devopsHost.removeSuffix("/")
        val request = "$devopsHost$ci$ciApi$userListApi"
        return try {
            val response = CanwayHttpUtils.doGet(request).content
            val canwayUserList = response.readJsonString<CanwayResponse<List<CanwayUser>>>().data
                ?: return listOf()
            canwayUserList.map {
                UserResult(userId = it.userId, name = it.displayName)
            }
        } catch (e: Exception) {
            logger.error("CI 权限中心用户列表接头调用失败: $request")
            super.listUserResult(rids)
        }
    }

    companion object {
        const val userListApi = "/service/blueking/user"
        private val logger: Logger = LoggerFactory.getLogger(CanwayUserServiceImpl::class.java)
    }
}

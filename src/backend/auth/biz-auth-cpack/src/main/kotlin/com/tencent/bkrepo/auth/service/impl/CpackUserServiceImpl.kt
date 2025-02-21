/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.auth.service.impl

import com.tencent.bkrepo.auth.constant.DEFAULT_PASSWORD
import com.tencent.bkrepo.auth.constant.PROJECT_MANAGE_PERMISSION
import com.tencent.bkrepo.auth.constant.PROJECT_VIEW_PERMISSION
import com.tencent.bkrepo.auth.dao.UserDao
import com.tencent.bkrepo.auth.dao.repository.RoleRepository
import com.tencent.bkrepo.auth.helper.UserHelper
import com.tencent.bkrepo.auth.message.AuthMessageCode
import com.tencent.bkrepo.auth.model.TUser
import com.tencent.bkrepo.auth.pojo.token.Token
import com.tencent.bkrepo.auth.pojo.token.TokenResult
import com.tencent.bkrepo.auth.pojo.user.CreateUserRequest
import com.tencent.bkrepo.auth.pojo.user.CreateUserToProjectRequest
import com.tencent.bkrepo.auth.pojo.user.CreateUserToRepoRequest
import com.tencent.bkrepo.auth.pojo.user.UpdateUserRequest
import com.tencent.bkrepo.auth.pojo.user.User
import com.tencent.bkrepo.auth.pojo.user.UserInfo
import com.tencent.bkrepo.auth.pojo.user.UserResult
import com.tencent.bkrepo.auth.service.PermissionService
import com.tencent.bkrepo.auth.service.UserService
import com.tencent.bkrepo.auth.util.DataDigestUtils
import com.tencent.bkrepo.auth.util.IDUtil
import com.tencent.bkrepo.auth.util.query.UserQueryHelper
import com.tencent.bkrepo.auth.util.request.UserRequestUtil
import com.tencent.bkrepo.common.api.constant.ANONYMOUS_USER
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.cpack.service.NotifyService
import com.tencent.bkrepo.common.metadata.service.project.ProjectService
import com.tencent.bkrepo.common.metadata.service.repo.RepositoryService
import com.tencent.bkrepo.common.metadata.util.DesensitizedUtils
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

open class CpackUserServiceImpl constructor(
    private val userDao: UserDao,
    roleRepository: RoleRepository,
    private val permissionService: PermissionService
) : UserService {

    @Autowired
    lateinit var repositoryClient: RepositoryService

    @Autowired
    lateinit var projectClient: ProjectService

    @Autowired
    lateinit var notifyService: NotifyService

    private val userHelper by lazy { UserHelper(userDao, roleRepository) }

    @Suppress("TooGenericExceptionCaught")
    override fun createUser(request: CreateUserRequest): Boolean {
        // todo 校验
        logger.info("create user request : [${DesensitizedUtils.toString(request)}]")
        // create a anonymous user is not allowed
        if (request.userId == ANONYMOUS_USER) {
            logger.warn("create user [${request.userId}]  is exist.")
            throw ErrorCodeException(AuthMessageCode.AUTH_DUP_UID)
        }
        val user = userDao.findFirstByUserId(request.userId)
        user?.let {
            logger.warn("create user [${request.userId}]  is exist.")
            throw ErrorCodeException(AuthMessageCode.AUTH_DUP_UID)
        }
        if (request.group && request.asstUsers.isEmpty()) {
            throw ErrorCodeException(AuthMessageCode.AUTH_ASST_USER_EMPTY)
        }
        var hashPwd: String = DataDigestUtils.md5FromStr(DEFAULT_PASSWORD)
        request.pwd?.let {
            hashPwd = DataDigestUtils.md5FromStr(request.pwd!!)
        }
        userDao.insert(
            TUser(
                userId = request.userId,
                name = request.name,
                pwd = hashPwd,
                admin = request.admin,
                locked = false,
                tokens = emptyList(),
                roles = emptyList(),
                asstUsers = request.asstUsers,
                group = request.group,
                email = request.email,
                phone = request.phone,
                createdDate = LocalDateTime.now(),
                lastModifiedDate = LocalDateTime.now()
            )
        )
        try {
            if (request.email != null && request.email!!.isNotBlank()) {
                notifyService.newAccountMessage(request.userId, request.name, listOf(request.email!!))
            }
        } catch (e: Exception) {
            logger.warn("send email failed", e)
        }
        return true
    }

    override fun createUserToRepo(request: CreateUserToRepoRequest): Boolean {
        logger.info("create user to repo request : [${DesensitizedUtils.toString(request)}]")
        repositoryClient.getRepoInfo(request.projectId, request.repoName) ?: run {
            logger.warn("repo [${request.projectId}/${request.repoName}]  not exist.")
            throw ErrorCodeException(AuthMessageCode.AUTH_REPO_NOT_EXIST)
        }
        // user not exist, create user
        try {
            val userResult = createUser(transferCreateRepoUserRequest(request))
            if (!userResult) {
                logger.warn("create user fail [$userResult]")
                return false
            }
        } catch (exception: ErrorCodeException) {
            if (exception.messageCode == AuthMessageCode.AUTH_DUP_UID) {
                return true
            }
            throw exception
        }
        return true
    }

    override fun createUserToProject(request: CreateUserToProjectRequest): Boolean {
        logger.info("create user to project request : [${DesensitizedUtils.toString(request)}]")
        projectClient.getProjectInfo(request.projectId) ?: run {
            logger.warn("project [${request.projectId}]  not exist.")
            throw ErrorCodeException(AuthMessageCode.AUTH_PROJECT_NOT_EXIST)
        }
        // user not exist, create user
        try {
            val userResult = createUser(transferCreateProjectUserRequest(request))
            if (!userResult) {
                logger.warn("create user fail [$userResult]")
                return false
            }
        } catch (exception: ErrorCodeException) {
            if (exception.messageCode == AuthMessageCode.AUTH_DUP_UID) {
                return true
            }
            throw exception
        }
        return true
    }

    override fun listUser(rids: List<String>): List<User> {
        logger.debug("list user rids : [$rids]")
        return if (rids.isEmpty()) {
            userDao.findAll().map { transferUser(it) }
        } else {
            userDao.findAllByRolesIn(rids).map { transferUser(it) }
        }
    }

    override fun deleteById(userId: String): Boolean {
        logger.info("delete user userId : [$userId]")
        userHelper.checkUserExist(userId)
        userDao.removeByUserId(userId)
        return true
    }

    override fun addUserToRole(userId: String, roleId: String): User? {
        logger.info("add user to role userId : [$userId, $roleId]")
        // check user
        userHelper.checkUserExist(userId)
        // check role
        userHelper.checkRoleExist(roleId)
        // check is role bind to role
        if (!userHelper.checkUserRoleBind(userId, roleId)) {
            userDao.addUserToRole(userId, roleId)
        }
        return getUserById(userId)
    }

    override fun addUserToRoleBatch(idList: List<String>, roleId: String): Boolean {
        logger.info("add user to role batch : [$idList, $roleId]")
        return userHelper.addUserToRoleBatchCommon(idList, roleId)
    }

    override fun removeUserFromRole(userId: String, roleId: String): User? {
        logger.info("remove user from role userId : [$userId], roleId : [$roleId]")
        // check user
        userHelper.checkUserExist(userId)
        // check role
        userHelper.checkRoleExist(roleId)
        userDao.removeUserFromRole(userId, roleId)
        return getUserById(userId)
    }

    override fun removeUserFromRoleBatch(idList: List<String>, roleId: String): Boolean {
        logger.info("remove user from role batch : [$idList, $roleId]")
        return userHelper.removeUserFromRoleBatchCommon(idList, roleId)
    }

    override fun updateUserById(userId: String, request: UpdateUserRequest): Boolean {
        logger.info("update user userId : [$userId], request : [$request]")
        userHelper.checkUserExist(userId)
        return userDao.updateUserById(userId, request)
    }

    override fun createToken(userId: String): Token? {
        logger.info("create token userId : [$userId]")
        val token = IDUtil.genRandomId()
        return addUserToken(userId, token, null)
    }

    override fun getUserPwdById(userId: String): String? {
        val tUser = userDao.findFirstByUserId(userId) ?: return null
        return tUser.pwd
    }

    override fun listValidToken(userId: String): List<Token> {
        logger.debug("list valid token : [$userId]")
        userHelper.checkUserExist(userId)
        return userDao.findFirstByUserId(userId)!!.tokens.filter {
            it.expiredAt == null || it.expiredAt!!.isAfter(LocalDateTime.now())
        }
    }

    override fun validateEntityUser(userId: String): Boolean {
        val user = userDao.findFirstByUserId(userId)
        return user != null && !user.group
    }

    override fun addUserToken(userId: String, name: String, expiredAt: String?): Token? {
        try {
            logger.info("add user token userId : [$userId] ,token : [$name]")
            userHelper.checkUserExist(userId)

            val existUserInfo = userDao.findFirstByUserId(userId)
            val existTokens = existUserInfo!!.tokens
            var id = UserRequestUtil.generateToken()
            var createdTime = LocalDateTime.now()
            existTokens.forEach {
                // 如果临时token已经存在，尝试更新token的过期时间
                if (it.name == name && it.expiredAt != null) {
                    // 先删除token
                    removeToken(userId, name)
                    id = it.id
                    createdTime = it.createdAt
                } else if (it.name == name && it.expiredAt == null) {
                    logger.warn("user token exist [$name]")
                    throw ErrorCodeException(AuthMessageCode.AUTH_USER_TOKEN_EXIST)
                }
            }
            // 创建token
            var expiredTime: LocalDateTime? = null
            if (expiredAt != null && expiredAt.isNotEmpty()) {
                val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                expiredTime = LocalDateTime.parse(expiredAt, dateTimeFormatter)
                // conv time
                expiredTime = expiredTime!!.plusHours(8)
            }
            val sm3Id = DataDigestUtils.sm3FromStr(id)
            val userToken = Token(name = name, id = id, createdAt = createdTime, expiredAt = expiredTime)
            val dataToken = Token(name = name, id = sm3Id, createdAt = createdTime, expiredAt = expiredTime)
            userDao.addUserToken(userId, dataToken)
            val userInfo = userDao.findFirstByUserId(userId)
            val tokens = userInfo!!.tokens
            tokens.forEach {
                if (it.name == name) return userToken
            }
            return null
        } catch (ignored: DateTimeParseException) {
            logger.error("add user token false [$ignored]")
            throw ErrorCodeException(AuthMessageCode.AUTH_USER_TOKEN_TIME_ERROR)
        }
    }

    override fun listUserToken(userId: String): List<TokenResult> {
        userHelper.checkUserExist(userId)
        val userInfo = getUserById(userId)
        val tokens = userInfo!!.tokens
        val result = mutableListOf<TokenResult>()
        tokens.forEach {
            result.add(TokenResult(it.name, it.createdAt, it.expiredAt))
        }
        return result
    }

    override fun listUserResult(rids: List<String>): List<UserResult> {
        return emptyList()
    }

    override fun removeToken(userId: String, name: String): Boolean {
        logger.info("remove token userId : [$userId] ,name : [$name]")
        userHelper.checkUserExist(userId)
        userDao.removeTokenFromUser(userId, name)
        return true
    }

    override fun getUserById(userId: String): User? {
        logger.debug("get user userId : [$userId]")
        val user = userDao.findFirstByUserId(userId) ?: return null
        return transferUser(user)
    }

    override fun findUserByUserToken(userId: String, pwd: String): User? {
        logger.debug("find user userId : [$userId]")
        val hashPwd = DataDigestUtils.md5FromStr(pwd)
        val sm3HashPwd = DataDigestUtils.sm3FromStr(pwd)
        val result = userDao.getUserByPassWordAndHash(userId, pwd, hashPwd, sm3HashPwd) ?: return null
        // password 匹配成功，返回
        if (result.pwd == hashPwd) {
            return UserRequestUtil.convToUser(result)
        }

        // token 匹配成功
        result.tokens.forEach {
            // 永久token，校验通过，临时token校验有效期
            if (UserRequestUtil.matchToken(pwd, sm3HashPwd, it.id) && it.expiredAt == null) {
                return UserRequestUtil.convToUser(result)
            } else if (UserRequestUtil.matchToken(pwd, sm3HashPwd, it.id) &&
                it.expiredAt != null && it.expiredAt!!.isAfter(LocalDateTime.now())
            ) {
                return UserRequestUtil.convToUser(result)
            }
        }

        return null
    }

    override fun userPage(
        pageNumber: Int,
        pageSize: Int,
        userName: String?,
        admin: Boolean?,
        locked: Boolean?
    ): Page<UserInfo> {
        val pageRequest = Pages.ofRequest(pageNumber, pageSize)
        val totalRecords = userDao.countByName(userName, admin, locked)
        val records = userDao.getByPage(userName, admin, locked, pageNumber, pageSize).map {
            UserRequestUtil.convToUserInfo(it)
        }
        return Pages.ofResponse(pageRequest, totalRecords, records)
    }

    override fun getRelatedUserById(userId: String): List<UserInfo> {
        return userDao.getUserByAsstUser(userId).map { UserRequestUtil.convToUserInfo(it) }
    }

    override fun userAll(
        userName: String?,
        admin: Boolean?,
        locked: Boolean?
    ): List<TUser> {
        val query = UserQueryHelper.getUserByName(userName, admin, locked)
        val records = userDao.find(query)
        return records
    }

    override fun getUserInfoById(userId: String): UserInfo? {
        val tUser = userDao.findFirstByUserId(userId) ?: return null
        return transferUserInfo(tUser)
    }

    override fun updatePassword(userId: String, oldPwd: String, newPwd: String): Boolean {
        logger.info("update user password : [$userId]")
        val hashOldPwd = DataDigestUtils.md5FromStr(oldPwd)
        val hashNewPwd = DataDigestUtils.md5FromStr(newPwd)
        val user = userDao.getByUserIdAndPassword(userId, hashOldPwd)
        user?.let {
            return userDao.updatePasswordByUserId(userId, hashNewPwd)
        }
        throw ErrorCodeException(CommonMessageCode.MODIFY_PASSWORD_FAILED, "modify password failed!")
    }

    override fun resetPassword(userId: String): Boolean {
        // todo 鉴权
        val newHashPwd = DataDigestUtils.md5FromStr(DEFAULT_PASSWORD)
        return userDao.updatePasswordByUserId(userId, newHashPwd)
    }

    override fun repeatUid(userId: String): Boolean {
        val user = userDao.findFirstByUserId(userId)
        return user != null
    }

    override fun listAdminUsers(): List<String> {
        return userDao.findAllAdminUsers().map { it.userId }
    }

    @Suppress("TooGenericExceptionCaught")
    override fun resetPassword(userId: String, newPwd: String?): Boolean {
        val targetPwd = if (newPwd != null && newPwd.isNotBlank()) {
            DataDigestUtils.md5FromStr(newPwd)
        } else {
            DataDigestUtils.md5FromStr(DEFAULT_PASSWORD)
        }
        userDao.updatePasswordByUserId(userId,targetPwd)
        getUserInfoById(userId)?.let { user ->
            try {
                if (user.email != null && user.email!!.isNotBlank()) {
                    notifyService.resetPwdMessage(user.userId, user.name, listOf(user.email!!))
                }
            } catch (e: Exception) {
                logger.warn("send email failed", e)
            }
        }
        return true
    }

    override fun listUserByProjectId(projectId: String, includeAdmin: Boolean): List<UserResult> {
        val permissions = permissionService.listProjectBuiltinPermission(projectId)
        val users = mutableSetOf<String>()
        val roles = mutableSetOf<String>()
        for (permission in permissions) {
            logger.info("$permission")
            if (projectBuiltinPermission.contains(permission.permName)) users.addAll(permission.users)
            if (permission.permName == PROJECT_VIEW_PERMISSION) roles.addAll(permission.roles)
        }
        for (role in roles) {
            users.addAll(listUserByRoleId(role).map { it.userId })
        }
        if (includeAdmin) users.addAll(listAdminUsers())
        val userList = listUser(listOf()).filter { users.contains(it.userId) }
        return userList.map { UserResult(userId = it.userId, name = it.name) }
    }

    private fun listUserByRoleId(id: String): Set<UserResult> {
        val result = mutableSetOf<UserResult>()
        userDao.findAllByRolesIn(listOf(id)).let { users ->
            for (user in users) {
                result.add(UserResult(user.userId, user.name))
            }
        }
        return result
    }

    override fun addUserAccount(userId: String, accountId: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun removeUserAccount(userId: String, accountId: String): Boolean {
        TODO("Not yet implemented")
    }

    private fun transferCreateRepoUserRequest(request: CreateUserToRepoRequest): CreateUserRequest {
        return CreateUserRequest(
            request.userId,
            request.name,
            request.pwd,
            request.admin,
            request.asstUsers,
            request.group
        )
    }

    private fun transferUserInfo(user: TUser): UserInfo {
        return UserInfo(
            userId = user.userId,
            name = user.name,
            locked = user.locked,
            email = user.email,
            phone = user.phone,
            createdDate = user.createdDate,
            admin = user.admin,
            group = user.group
        )
    }

    fun transferCreateProjectUserRequest(request: CreateUserToProjectRequest): CreateUserRequest {
        return CreateUserRequest(
            request.userId,
            request.name,
            request.pwd,
            request.admin,
            request.asstUsers,
            request.group
        )
    }

    private fun transferUser(user: TUser): User {
        return User(
            userId = user.userId,
            name = user.name,
            pwd = user.pwd,
            admin = user.admin,
            locked = user.locked,
            tokens = user.tokens,
            roles = user.roles
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CpackUserServiceImpl::class.java)
        val projectBuiltinPermission = listOf(PROJECT_MANAGE_PERMISSION, PROJECT_VIEW_PERMISSION)
    }
}

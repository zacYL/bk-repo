package com.tencent.bkrepo.auth.service.impl

import com.mongodb.BasicDBObject
import com.tencent.bkrepo.auth.ciPermission
import com.tencent.bkrepo.auth.ciApi
import com.tencent.bkrepo.auth.ciUserManager
import com.tencent.bkrepo.auth.constant.DEFAULT_PASSWORD
import com.tencent.bkrepo.auth.message.AuthMessageCode
import com.tencent.bkrepo.auth.model.TUser
import com.tencent.bkrepo.auth.pojo.CanwayUser
import com.tencent.bkrepo.auth.pojo.DevopsUser
import com.tencent.bkrepo.auth.pojo.token.Token
import com.tencent.bkrepo.auth.pojo.token.TokenResult
import com.tencent.bkrepo.auth.pojo.user.CreateUserRequest
import com.tencent.bkrepo.auth.pojo.user.CreateUserToRepoRequest
import com.tencent.bkrepo.auth.pojo.user.CreateUserToProjectRequest
import com.tencent.bkrepo.auth.pojo.user.User
import com.tencent.bkrepo.auth.pojo.user.UpdateUserRequest
import com.tencent.bkrepo.auth.pojo.user.UserInfo
import com.tencent.bkrepo.auth.pojo.user.UserResult
import com.tencent.bkrepo.auth.repository.RoleRepository
import com.tencent.bkrepo.auth.repository.UserRepository
import com.tencent.bkrepo.auth.service.local.UserServiceImpl
import com.tencent.bkrepo.auth.util.DataDigestUtils
import com.tencent.bkrepo.auth.util.IDUtil
import com.tencent.bkrepo.common.api.constant.ANONYMOUS_USER
import com.tencent.bkrepo.common.api.constant.USER_KEY
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.sensitive.DesensitizedUtils
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.common.devops.api.conf.DevopsConf
import com.tencent.bkrepo.common.devops.api.pojo.response.CanwayResponse
import com.tencent.bkrepo.common.devops.api.util.http.CanwayHttpUtils
import com.tencent.bkrepo.common.mongo.dao.util.Pages
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class CanwayUserServiceImpl(
    private val userRepository: UserRepository,
    roleRepository: RoleRepository,
    private val mongoTemplate: MongoTemplate
) : UserServiceImpl(userRepository, roleRepository, mongoTemplate) {

    @Autowired
    lateinit var devopsConf: DevopsConf

    override fun createUser(request: CreateUserRequest): Boolean {
        // todo 校验
        logger.info("create user request : [${DesensitizedUtils.toString(request)}]")
        val user = userRepository.findFirstByUserId(request.userId)
        user?.let {
            logger.warn("create user [${request.userId}]  is exist.")
            throw ErrorCodeException(AuthMessageCode.AUTH_DUP_UID)
        }
        if (request.group && request.asstUsers.isEmpty()) {
            throw ErrorCodeException(AuthMessageCode.AUTH_ASST_USER_EMPTY)
        }
        var pwd: String = DataDigestUtils.md5FromStr(DEFAULT_PASSWORD)
        request.pwd?.let {
            pwd = DataDigestUtils.md5FromStr(request.pwd!!)
        }
        userRepository.insert(
            TUser(
                userId = request.userId,
                name = request.name,
                pwd = pwd,
                admin = request.admin,
                locked = false,
                tokens = emptyList(),
                roles = emptyList(),
                asstUsers = request.asstUsers,
                group = request.group
            )
        )
        return true
    }

    override fun createUserToRepo(request: CreateUserToRepoRequest): Boolean {
        logger.info("create user to repo request : [${DesensitizedUtils.toString(request)}]")
        repositoryClient.getRepoInfo(request.projectId, request.repoName).data ?: run {
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
        projectClient.getProjectInfo(request.projectId).data ?: run {
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
            userRepository.findAll().map { transferUser(it) }
        } else {
            userRepository.findAllByRolesIn(rids).map { transferUser(it) }
        }
    }

    override fun deleteById(userId: String): Boolean {
        logger.info("delete user userId : [$userId]")
        checkUserExist(userId)
        userRepository.deleteByUserId(userId)
        val operator = HttpContextHolder.getRequest().getAttribute(USER_KEY) as? String ?: ANONYMOUS_USER
        return true
    }

    override fun addUserToRole(userId: String, roleId: String): User? {
        logger.info("add user to role userId : [$userId], roleId : [$roleId]")
        // check user
        checkUserExist(userId)
        // check role
        checkRoleExist(roleId)
        // check is role bind to role
        val query = Query()
        val update = Update()
        if (!checkUserRoleBind(userId, roleId)) {
            query.addCriteria(Criteria.where(TUser::userId.name).`is`(userId))
            update.addToSet(TUser::roles.name, roleId)
            mongoTemplate.upsert(query, update, TUser::class.java)
        }
        return getUserById(userId)
    }

    override fun addUserToRoleBatch(idList: List<String>, roleId: String): Boolean {
        logger.info("add user to role batch userId : [$idList], roleId : [$roleId]")
        checkUserExistBatch(idList)
        checkRoleExist(roleId)
        val query = Query()
        val update = Update()
        query.addCriteria(Criteria.where(TUser::userId.name).`in`(idList))
        update.addToSet(TUser::roles.name, roleId)
        mongoTemplate.updateMulti(query, update, TUser::class.java)
        return true
    }

    override fun removeUserFromRole(userId: String, roleId: String): User? {
        logger.info("remove user from role userId : [$userId], roleId : [$roleId]")
        // check user
        checkUserExist(userId)
        // check role
        checkRoleExist(roleId)
        val query = Query()
        val update = Update()
        query.addCriteria(Criteria.where(TUser::userId.name).`is`(userId).and(TUser::roles.name).`is`(roleId))
        update.unset("roles.$")
        mongoTemplate.upsert(query, update, TUser::class.java)
        return getUserById(userId)
    }

    override fun removeUserFromRoleBatch(idList: List<String>, roleId: String): Boolean {
        logger.info("remove user from role  batch userId : [$idList], roleId : [$roleId]")
        checkUserExistBatch(idList)
        checkRoleExist(roleId)
        val query = Query()
        val update = Update()
        query.addCriteria(Criteria.where(TUser::userId.name).`in`(idList).and(TUser::roles.name).`is`(roleId))
        update.unset("roles.$")
        val result = mongoTemplate.updateMulti(query, update, TUser::class.java)
        if (result.modifiedCount == 1L) return true
        return false
    }

    override fun updateUserById(userId: String, request: UpdateUserRequest): Boolean {
        logger.info("update user userId : [$userId], request : [$request]")
        checkUserExist(userId)

        val query = Query()
        query.addCriteria(Criteria.where(TUser::userId.name).`is`(userId))
        val update = Update()
        request.pwd?.let {
            val pwd = DataDigestUtils.md5FromStr(request.pwd!!)
            update.set(TUser::pwd.name, pwd)
        }
        request.admin?.let {
            update.set(TUser::admin.name, request.admin!!)
        }
        request.name?.let {
            update.set(TUser::name.name, request.name!!)
        }
        val result = mongoTemplate.updateFirst(query, update, TUser::class.java)
        if (result.modifiedCount == 1L) return true
        return false
    }

    override fun createToken(userId: String): Token? {
        logger.info("create token userId : [$userId]")
        val token = IDUtil.genRandomId()
        return addUserToken(userId, token, null)
    }

    override fun addUserToken(userId: String, name: String, expiredAt: String?): Token? {
        try {
            logger.info("add user token userId : [$userId] ,token : [$name]")
            checkUserExist(userId)

            val existUserInfo = getUserById(userId)
            val existTokens = existUserInfo!!.tokens
            existTokens.forEach {
                if (it.name == name) {
                    logger.warn("user token exist [$name]")
                    throw ErrorCodeException(AuthMessageCode.AUTH_USER_TOKEN_EXIST)
                }
            }
            val query = Query.query(Criteria.where(TUser::userId.name).`is`(userId))
            val update = Update()
            val id = IDUtil.genRandomId()
            val now = LocalDateTime.now()
            var expiredTime: LocalDateTime? = null
            if (expiredAt != null && expiredAt.length != 0) {
                val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                expiredTime = LocalDateTime.parse(expiredAt, dateTimeFormatter)
                // conv time
                expiredTime = expiredTime!!.plusHours(8)
            }
            val userToken = Token(
                name = name,
                id = id,
                createdAt = now,
                expiredAt = expiredTime
            )
            update.addToSet(TUser::tokens.name, userToken)
            mongoTemplate.upsert(query, update, TUser::class.java)
            val userInfo = getUserById(userId)
            val tokens = userInfo!!.tokens
            tokens.forEach {
                if (it.name == name) {
                    return it
                }
            }
            return null
        } catch (ignored: DateTimeParseException) {
            logger.error("add user token false [$ignored]")
            throw ErrorCodeException(AuthMessageCode.AUTH_USER_TOKEN_ERROR)
        }
    }

    override fun listUserToken(userId: String): List<TokenResult> {
        checkUserExist(userId)
        val userInfo = getUserById(userId)
        val tokens = userInfo!!.tokens
        val result = mutableListOf<TokenResult>()
        tokens.forEach {
            result.add(
                TokenResult(
                    it.name,
                    it.createdAt,
                    it.expiredAt
                )
            )
        }
        return result
    }

    override fun removeToken(userId: String, name: String): Boolean {
        logger.info("remove token userId : [$userId] ,name : [$name]")
        checkUserExist(userId)
        val query = Query.query(Criteria.where(TUser::userId.name).`is`(userId))
        val s = BasicDBObject()
        s["name"] = name
        val update = Update()
        update.pull(TUser::tokens.name, s)
        mongoTemplate.updateFirst(query, update, TUser::class.java)
        return true
    }

    override fun getUserById(userId: String): User? {
        logger.debug("get user userId : [$userId]")
        val user = userRepository.findFirstByUserId(userId) ?: return null
        return transferUser(user)
    }

    override fun findUserByUserToken(userId: String, pwd: String): User? {
        logger.debug("find user userId : [$userId]")
        val hashPwd = DataDigestUtils.md5FromStr(pwd)
        val criteria = Criteria()
        criteria.orOperator(Criteria.where(TUser::pwd.name).`is`(hashPwd), Criteria.where("tokens.id").`is`(pwd))
            .and(TUser::userId.name).`is`(userId)
        val query = Query.query(criteria)
        val result = mongoTemplate.findOne(query, TUser::class.java) ?: return null
        return transferUser(result)
    }

    override fun userPage(
        pageNumber: Int,
        pageSize: Int,
        user: String?,
        admin: Boolean?,
        locked: Boolean?
    ): Page<UserInfo> {
        val criteria = Criteria()
        user?.let {
            val userRegex = PathUtils.escapeRegex(user)
            criteria.orOperator(
                Criteria.where(TUser::userId.name).regex("^$userRegex"),
                Criteria.where(TUser::name.name).regex("^$userRegex")
            )
        }
        admin?.let { criteria.and(TUser::admin.name).`is`(admin) }
        locked?.let { criteria.and(TUser::locked.name).`is`(locked) }
        val query = Query.query(criteria)
        val pageRequest = Pages.ofRequest(pageNumber, pageSize)
        val totalRecords = mongoTemplate.count(query, TUser::class.java)
        val records = mongoTemplate.find(query.with(pageRequest), TUser::class.java).map { transferUserInfo(it) }
        return Pages.ofResponse(pageRequest, totalRecords, records)
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override fun listUserResult(rids: List<String>): List<UserResult> {
        val devopsHost = devopsConf.devopsHost.removeSuffix("/")
        var request = "$devopsHost$ciUserManager$ciApi$userListApi_4300"
        return try {
            val response = CanwayHttpUtils.doGet(request).content
            val canwayUserList = response.readJsonString<CanwayResponse<List<DevopsUser>>>().data
                ?: return listOf()
            canwayUserList.map {
                UserResult(userId = it.id, name = it.displayName)
            }
        } catch (e: Exception) {
            logger.warn("CI 用户管理用户列表接头调用失败: $request")
            try {
                request = "$devopsHost$ciPermission$ciApi$userListApi"
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
    }

    companion object {
        const val userListApi = "/service/blueking/user"
        // 权限中心4300接口
        const val userListApi_4300 = "/service/user/allUser"
        private val logger: Logger = LoggerFactory.getLogger(CanwayUserServiceImpl::class.java)
    }
}

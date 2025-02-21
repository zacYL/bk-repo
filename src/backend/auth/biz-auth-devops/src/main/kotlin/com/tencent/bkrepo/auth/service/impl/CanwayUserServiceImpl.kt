package com.tencent.bkrepo.auth.service.impl

import com.mongodb.BasicDBObject
import com.tencent.bkrepo.auth.api.CanwayUsermangerClient
import com.tencent.bkrepo.auth.CI_API
import com.tencent.bkrepo.auth.CI_USER_MANAGER
import com.tencent.bkrepo.auth.constant.AUTH_ADMIN
import com.tencent.bkrepo.auth.constant.DEFAULT_PASSWORD
import com.tencent.bkrepo.auth.dao.UserDao
import com.tencent.bkrepo.auth.dao.repository.RoleRepository
import com.tencent.bkrepo.auth.helper.UserHelper
import com.tencent.bkrepo.auth.message.AuthMessageCode
import com.tencent.bkrepo.auth.model.TUser
import com.tencent.bkrepo.auth.pojo.DevopsUser
import com.tencent.bkrepo.auth.pojo.UserLoginVo
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
import com.tencent.bkrepo.auth.util.DataDigestUtils
import com.tencent.bkrepo.auth.util.IDUtil
import com.tencent.bkrepo.auth.util.request.UserRequestUtil
import com.tencent.bkrepo.common.api.constant.StringPool.EMPTY
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.artifact.path.PathUtils
import com.tencent.bkrepo.common.devops.client.DevopsClient
import com.tencent.bkrepo.common.devops.conf.DevopsConf
import com.tencent.bkrepo.common.devops.pojo.response.CanwayResponse
import com.tencent.bkrepo.common.devops.util.http.SimpleHttpUtils
import com.tencent.bkrepo.common.metadata.util.DesensitizedUtils
import com.tencent.bkrepo.common.mongo.dao.util.Pages
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
    private val userDao: UserDao,
    roleRepository: RoleRepository,
    private val mongoTemplate: MongoTemplate,
    permissionService: PermissionService,
    private val canwayUsermangerClient: CanwayUsermangerClient
) : CpackUserServiceImpl(userDao, roleRepository, permissionService) {

    @Autowired
    lateinit var devopsConf: DevopsConf

    @Autowired
    lateinit var devopsClient: DevopsClient

    private val userHelper by lazy { UserHelper(userDao, roleRepository) }

    override fun createUser(request: CreateUserRequest): Boolean {
        // todo 校验
        logger.info("create user request : [${DesensitizedUtils.toString(request)}]")
        val user = userDao.findFirstByUserId(request.userId)
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
        userDao.insert(
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
        repositoryClient.getRepoInfo(request.projectId, request.repoName) ?: run {
            logger.warn("repo [${request.projectId}/${request.repoName}]  not exist.")
            throw ErrorCodeException(AuthMessageCode.AUTH_REPO_NOT_EXIST)
        }
        // user not exist, create user
        try {
            val userResult = createUser(UserRequestUtil.convToCreateRepoUserRequest(request))
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
        logger.debug("list user rids : [{}]", rids)
        return if (rids.isEmpty()) {
            userDao.findAll().map { UserRequestUtil.convToUser(it) }
        } else {
            userDao.findAllByRolesIn(rids).map { UserRequestUtil.convToUser(it) }
        }
    }

    override fun deleteById(userId: String): Boolean {
        logger.info("delete user userId : [$userId]")
        userHelper.checkUserExist(userId)
        userDao.removeByUserId(userId)
        return true
    }

    override fun addUserToRole(userId: String, roleId: String): User? {
        logger.info("add user to role userId : [$userId], roleId : [$roleId]")
        // check user
        userHelper.checkUserExist(userId)
        // check role
        userHelper.checkRoleExist(roleId)
        // check is role bind to role
        val query = Query()
        val update = Update()
        if (!userHelper.checkUserRoleBind(userId, roleId)) {
            query.addCriteria(Criteria.where(TUser::userId.name).`is`(userId))
            update.addToSet(TUser::roles.name, roleId)
            mongoTemplate.upsert(query, update, TUser::class.java)
        }
        return getUserById(userId)
    }

    override fun addUserToRoleBatch(idList: List<String>, roleId: String): Boolean {
        logger.info("add user to role batch userId : [$idList], roleId : [$roleId]")
        userHelper.checkUserExistBatch(idList)
        userHelper.checkRoleExist(roleId)
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
        userHelper.checkUserExist(userId)
        // check role
        userHelper.checkRoleExist(roleId)
        val query = Query()
        val update = Update()
        query.addCriteria(Criteria.where(TUser::userId.name).`is`(userId).and(TUser::roles.name).`is`(roleId))
        update.unset("roles.$")
        mongoTemplate.upsert(query, update, TUser::class.java)
        return getUserById(userId)
    }

    override fun removeUserFromRoleBatch(idList: List<String>, roleId: String): Boolean {
        logger.info("remove user from role  batch userId : [$idList], roleId : [$roleId]")
        userHelper.checkUserExistBatch(idList)
        userHelper.checkRoleExist(roleId)
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
        userHelper.checkUserExist(userId)

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
            userHelper.checkUserExist(userId)

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
            if (expiredAt != null && expiredAt.isNotEmpty()) {
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
        userHelper.checkUserExist(userId)
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
        userHelper.checkUserExist(userId)
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
        val user = userDao.findFirstByUserId(userId) ?: return null
        return UserRequestUtil.convToUser(user)
    }

    override fun findUserByUserToken(userId: String, pwd: String): User? {
        logger.debug("find user userId : [$userId]")
        val isLogin = canwayUsermangerClient.login(UserLoginVo(userId, pwd)).data
        return if (isLogin == true) {
            UserRequestUtil.convToUser(
                TUser(
                    userId = AUTH_ADMIN,
                    name = EMPTY,
                    pwd = EMPTY
                )
            )
        } else {
            null
        }
//        val hashPwd = DataDigestUtils.md5FromStr(pwd)
//        val criteria = Criteria()
//        criteria.orOperator(Criteria.where(TUser::pwd.name).`is`(hashPwd), Criteria.where("tokens.id").`is`(pwd))
//            .and(TUser::userId.name).`is`(userId)
//        val query = Query.query(criteria)
//        val result = mongoTemplate.findOne(query, TUser::class.java) ?: return null
//        return transferUser(result)
    }

    override fun userPage(
        pageNumber: Int,
        pageSize: Int,
        userName: String?,
        admin: Boolean?,
        locked: Boolean?
    ): Page<UserInfo> {
        val criteria = Criteria()
        userName?.let {
            val userRegex = PathUtils.escapeRegex(userName)
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
        val records =
            mongoTemplate.find(query.with(pageRequest), TUser::class.java).map { UserRequestUtil.convToUserInfo(it) }
        return Pages.ofResponse(pageRequest, totalRecords, records)
    }

    override fun userAll(
        userName: String?,
        admin: Boolean?,
        locked: Boolean?
    ): List<TUser> {
        val criteria = Criteria()
        userName?.let {
            val userRegex = PathUtils.escapeRegex(userName)
            criteria.orOperator(
                Criteria.where(TUser::userId.name).regex("^$userRegex"),
                Criteria.where(TUser::name.name).regex("^$userRegex")
            )
        }
        admin?.let { criteria.and(TUser::admin.name).`is`(admin) }
        locked?.let { criteria.and(TUser::locked.name).`is`(locked) }
        val query = Query.query(criteria)
        val records = mongoTemplate.find(query, TUser::class.java)
        return records
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    override fun listUserResult(rids: List<String>): List<UserResult> {
        val devopsHost = devopsConf.devopsHost.removeSuffix("/")
        val request = "$devopsHost$CI_USER_MANAGER$CI_API$userListApi"
        return try {
            val response = SimpleHttpUtils.doGet(request).content
            val canwayUserList = response.readJsonString<CanwayResponse<List<DevopsUser>>>().data
                ?: return listOf()
            canwayUserList.map {
                UserResult(userId = it.id, name = it.displayName)
            }
        } catch (e: Exception) {
            logger.warn("CI 用户管理用户列表接头调用失败: $request")
            super.listUserResult(rids)
        }
    }

    override fun listUserByProjectId(projectId: String, includeAdmin: Boolean): List<UserResult> {
//        val localUsers = listUserResult(emptyList())
//        val projectUsers = devopsClient.usersByProjectId(projectId)?.map { it.userId } ?: listOf()
        return devopsClient.usersByProjectId(projectId)?.map { UserResult(it.userId, it.username) } ?: listOf()
    }

    companion object {
        const val userListApi = "/service/user/all"
        private val logger: Logger = LoggerFactory.getLogger(CanwayUserServiceImpl::class.java)
    }
}

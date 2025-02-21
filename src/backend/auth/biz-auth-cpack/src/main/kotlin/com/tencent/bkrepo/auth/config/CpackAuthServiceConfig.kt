package com.tencent.bkrepo.auth.config

import com.tencent.bkrepo.auth.dao.AccountDao
import com.tencent.bkrepo.auth.dao.PermissionDao
import com.tencent.bkrepo.auth.dao.PersonalPathDao
import com.tencent.bkrepo.auth.dao.RepoAuthConfigDao
import com.tencent.bkrepo.auth.dao.UserDao
import com.tencent.bkrepo.auth.dao.repository.RoleRepository
import com.tencent.bkrepo.auth.service.PermissionService
import com.tencent.bkrepo.auth.service.RoleService
import com.tencent.bkrepo.auth.service.UserService
import com.tencent.bkrepo.auth.service.impl.CpackPermissionServiceImpl
import com.tencent.bkrepo.auth.service.impl.CpackRoleServiceImpl
import com.tencent.bkrepo.auth.service.impl.CpackUserServiceImpl
import com.tencent.bkrepo.common.metadata.service.project.ProjectService
import com.tencent.bkrepo.common.metadata.service.repo.RepositoryService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.data.mongodb.core.MongoTemplate

@Configuration
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class CpackAuthServiceConfig {

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["realm"], havingValue = "cpack")
    fun cpackPermissionService(
        @Autowired accountDao: AccountDao,
        @Autowired userDao: UserDao,
        @Autowired roleRepository: RoleRepository,
        @Autowired permissionDao: PermissionDao,
        @Autowired personalPathDao: PersonalPathDao,
        @Autowired repoAuthConfigDao: RepoAuthConfigDao,
        @Autowired repositoryClient: RepositoryService,
        @Autowired projectClient: ProjectService
    ): PermissionService {
        logger.debug("init cpackPermissionServiceImpl")
        return CpackPermissionServiceImpl(
            accountDao,
            userDao,
            roleRepository,
            permissionDao,
            personalPathDao,
            repoAuthConfigDao,
            repositoryClient,
            projectClient
        )
    }

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["realm"], havingValue = "cpack")
    fun cpackRoleService(
        @Autowired roleRepository: RoleRepository,
        @Autowired userRepository: UserDao,
        @Autowired userService: UserService,
        @Autowired mongoTemplate: MongoTemplate,
        @Autowired permissionService: PermissionService
    ): RoleService {
        logger.debug("init cpackRoleServiceImpl")
        return CpackRoleServiceImpl(
            roleRepository,
            userService,
            userRepository,
            mongoTemplate,
            permissionService
        )
    }

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["realm"], havingValue = "cpack")
    fun cpackUserService(
        @Autowired userRepository: UserDao,
        @Autowired roleRepository: RoleRepository,
        @Autowired mongoTemplate: MongoTemplate,
        @Autowired permissionService: PermissionService
    ): UserService {
        logger.debug("init cpackUserServiceImpl")
        return CpackUserServiceImpl(
            userRepository,
            roleRepository,
            mongoTemplate,
            permissionService
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CpackAuthServiceConfig::class.java)
    }
}

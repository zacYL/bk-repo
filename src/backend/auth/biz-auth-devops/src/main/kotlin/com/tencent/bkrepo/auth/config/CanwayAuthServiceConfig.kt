package com.tencent.bkrepo.auth.config

import com.tencent.bkrepo.auth.api.CanwayUsermangerClient
import com.tencent.bkrepo.auth.dao.*
import com.tencent.bkrepo.auth.dao.repository.RoleRepository
import com.tencent.bkrepo.auth.general.DevOpsAuthGeneral
import com.tencent.bkrepo.auth.service.PermissionService
import com.tencent.bkrepo.auth.service.RoleService
import com.tencent.bkrepo.auth.service.UserService
import com.tencent.bkrepo.auth.service.impl.CanwayPermissionServiceImpl
import com.tencent.bkrepo.auth.service.impl.CanwayRoleServiceImpl
import com.tencent.bkrepo.auth.service.impl.CanwayUserServiceImpl
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
class CanwayAuthServiceConfig {

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["realm"], havingValue = "canway")
    fun canwayPermissionService(
        @Autowired accountDao: AccountDao,
        @Autowired userDao: UserDao,
        @Autowired roleRepository: RoleRepository,
        @Autowired permissionDao: PermissionDao,
        @Autowired personalPathDao: PersonalPathDao,
        @Autowired repoAuthConfigDao: RepoAuthConfigDao,
        @Autowired mongoTemplate: MongoTemplate,
        @Autowired repositoryClient: RepositoryService,
        @Autowired projectClient: ProjectService,
        @Autowired devOpsAuthGeneral: DevOpsAuthGeneral,
    ): PermissionService {
        logger.debug("init CanwayPermissionServiceImpl")
        return CanwayPermissionServiceImpl(
            accountDao,
            userDao,
            roleRepository,
            permissionDao,
            personalPathDao,
            repoAuthConfigDao,
            devOpsAuthGeneral,
            repositoryClient,
            projectClient
        )
    }

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["realm"], havingValue = "canway")
    fun canwayRoleService(
        @Autowired roleRepository: RoleRepository,
        @Autowired userRepository: UserDao,
        @Autowired userService: UserService,
        @Autowired mongoTemplate: MongoTemplate,
        @Autowired permissionService: PermissionService
    ): RoleService {
        logger.debug("init CanwayRoleServiceImpl")
        return CanwayRoleServiceImpl(
            roleRepository,
            userService,
            userRepository,
            mongoTemplate,
            permissionService
        )
    }

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["realm"], havingValue = "canway")
    fun canwayUserService(
        @Autowired userRepository: UserDao,
        @Autowired roleRepository: RoleRepository,
        @Autowired mongoTemplate: MongoTemplate,
        @Autowired permissionService: PermissionService,
        @Autowired canwayUsermangerClient: CanwayUsermangerClient
    ): UserService {
        logger.debug("init CanwayUserServiceImpl")
        return CanwayUserServiceImpl(
            userRepository,
            roleRepository,
            mongoTemplate,
            permissionService,
            canwayUsermangerClient
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CanwayAuthServiceConfig::class.java)
    }
}

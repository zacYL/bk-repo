package com.tencent.bkrepo.auth.service.canway.conf

import com.tencent.bkrepo.auth.repository.PermissionRepository
import com.tencent.bkrepo.auth.repository.RoleRepository
import com.tencent.bkrepo.auth.repository.UserRepository
import com.tencent.bkrepo.auth.service.DepartmentService
import com.tencent.bkrepo.auth.service.PermissionService
import com.tencent.bkrepo.auth.service.RoleService
import com.tencent.bkrepo.auth.service.UserService
import com.tencent.bkrepo.auth.service.canway.CanwayPermissionServiceImpl
import com.tencent.bkrepo.auth.service.canway.CanwayRoleServiceImpl
import com.tencent.bkrepo.auth.service.canway.bk.BkUserService
import com.tencent.bkrepo.repository.api.ProjectClient
import com.tencent.bkrepo.repository.api.RepositoryClient
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
        @Autowired userRepository: UserRepository,
        @Autowired roleRepository: RoleRepository,
        @Autowired permissionRepository: PermissionRepository,
        @Autowired mongoTemplate: MongoTemplate,
        @Autowired repositoryClient: RepositoryClient,
        @Autowired canwayAuthConf: CanwayAuthConf,
        @Autowired departmentService: DepartmentService,
        @Autowired bkUserService: BkUserService,
        @Autowired projectClient: ProjectClient
    ): PermissionService {
        logger.debug("init CanwayPermissionServiceImpl")
        return CanwayPermissionServiceImpl(
            userRepository,
            roleRepository,
            permissionRepository,
            mongoTemplate,
            repositoryClient,
            canwayAuthConf,
            departmentService,
            bkUserService,
            projectClient
        )
    }

    @Bean
    @ConditionalOnProperty(prefix = "auth", name = ["realm"], havingValue = "canway")
    fun canwayRoleService(
        @Autowired roleRepository: RoleRepository,
        @Autowired userRepository: UserRepository,
        @Autowired userService: UserService,
        @Autowired mongoTemplate: MongoTemplate,
        @Autowired canwayAuthConf: CanwayAuthConf
    ): RoleService {
        logger.debug("init CanwayRoleServiceImpl")
        return CanwayRoleServiceImpl(
            roleRepository,
            userService,
            userRepository,
            mongoTemplate,
            canwayAuthConf
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CanwayAuthServiceConfig::class.java)
    }
}

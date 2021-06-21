package com.tencent.bkrepo.common.devops.enum

enum class CanwayAuditType(val value: String) {
    REPOSITORY_CREATE("bkrepo_repository_create"),
    REPOSITORY_DELETE("bkrepo_repository_delete"),
    REPOSITORY_UPDATE("bkrepo_repository_update"),

    NODE_CREATE("bkrepo_node_create"),
    NODE_DELETE("bkrepo_node_delete"),
    NODE_RENAME("bkrepo_node_rename"),
    NODE_UPDATE("bkrepo_node_update"),
    NODE_MOVE("bkrepo_node_move"),
    NODE_COPY("bkrepo_node_copy"),

    PACKAGE_CREATE("bkrepo_package_create"),
    PACKAGE_DELETE("bkrepo_package_delete"),
    PACKAGE_UPDATE("bkrepo_package_update"),
    PACKAGE_STAGE("bkrepo_package_stage"),
    PACKAGE_DOWNLOAD("bkrepo_package_download"),
}

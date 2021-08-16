package com.tencent.bkrepo.common.api.event.base

/**
 * 事件类型
 */
enum class EventType(val nick: String) {
    // PROJECT
    PROJECT_CREATED("创建项目"),

    // REPOSITORY
    REPO_CREATED("创建仓库"),
    REPO_UPDATED("更新仓库"),
    REPO_DELETED("删除仓库"),

    // NODE
    NODE_CREATED("创建节点"),
    NODE_RENAMED("重命名节点"),
    NODE_MOVED("移动节点"),
    NODE_COPIED("复制节点"),
    NODE_DELETED("删除节点"),

    // METADATA
    METADATA_DELETED("删除元数据"),
    METADATA_SAVED("保存元数据"),

    // PACKAGE

    // VERSION
    VERSION_CREATED("创建制品"),
    VERSION_DELETED("删除制品"),
    VERSION_DOWNLOAD("下载制品"),
    VERSION_UPDATED("更新制品"),
    VERSION_STAGED("晋级制品"),

    // ADMIN
    ADMIN_ADD("添加管理员"),
    ADMIN_DELETE("移除管理员")
}

package com.tencent.bkrepo.auth.listener.event

import com.tencent.bkrepo.common.api.event.base.EventType

open class AuthEvent(
    /**
     * 事件类型
     */
    open val type: EventType,
    /**
     * 项目id
     */
    open val projectId: String? = null,
    /**
     * 仓库名称
     */
    open val repoName: String? = null,
    /**
     * 事件资源key，具有唯一性
     * ex:
     * 1. 节点类型对应fullPath
     * 2. 仓库类型对应仓库名称
     * 3. 包类型对应包名称
     */
    open val resourceKey: String,
    /**
     * 操作用户
     */
    open val userId: String,
    /**
     * 附属数据
     */
    open val data: Map<String, Any> = mapOf()

)

package com.tencent.bkrepo.repository.util

import com.tencent.bkrepo.repository.pojo.log.OperateType
import com.tencent.bkrepo.repository.pojo.log.ResourceType

object OperateMapUtils {
    fun transferOperationType(resourceType: ResourceType, operateType: OperateType): String {
        return if (resourceType == ResourceType.PACKAGE) {
            when (operateType) {
                OperateType.CREATE -> "上传"
                OperateType.DELETE -> "删除"
                OperateType.DOWNLOAD -> "下载"
                OperateType.UPDATE -> "更新"
                OperateType.STAGE -> "晋级"
                else -> "unknown operate"
            }
        } else if (resourceType == ResourceType.REPOSITORY) {
            when (operateType) {
                OperateType.CREATE -> "创建"
                OperateType.DELETE -> "删除"
                OperateType.UPDATE -> "更新"
                else -> "unknown operate"
            }
        } else if (resourceType == ResourceType.USER) {
            when (operateType) {
                OperateType.CREATE -> "创建"
                else -> "unknown operate"
            }
        } else if (resourceType == ResourceType.ADMIN) {
            when (operateType) {
                OperateType.CREATE -> "添加"
                OperateType.DELETE -> "移除"
                else -> "unknown operate"
            }
        } else {
            "unknown resource"
        }
    }

    fun transferResourceType(resourceType: ResourceType): String {
        return when (resourceType) {
            ResourceType.PROJECT -> "项目"
            ResourceType.REPOSITORY -> "仓库"
            ResourceType.USER -> "用户"
            ResourceType.PACKAGE -> "包"
            ResourceType.NODE -> "节点"
            ResourceType.METADATA -> "元数据"
            ResourceType.ADMIN -> "管理员"
        }
    }
}

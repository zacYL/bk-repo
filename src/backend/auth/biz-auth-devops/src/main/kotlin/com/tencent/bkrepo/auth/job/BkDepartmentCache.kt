package com.tencent.bkrepo.auth.job

import com.tencent.bkrepo.common.devops.client.BkClient
import com.tencent.bkrepo.common.devops.pojo.BkChildrenDepartment
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "auth", name = ["realm"], havingValue = "canway", matchIfMissing = true)
class BkDepartmentCache(
    private val bkClient: BkClient
) {

    lateinit var allBkDepartment: List<BkChildrenDepartment>

    @Scheduled(fixedDelay = 5 * 60 * 1000L)
    fun departments() {
        logger.info("start fetch blueking department")
        for (i in 0..2) {
            fetchBkDepartment()
            if (allBkDepartment.isNullOrEmpty()) {
                logger.info("fetched blueking department is null, will retry in 100ms, $i")
                Thread.sleep(100)
            } else {
                logger.info("fetch blueking department success, size: ${allBkDepartment.size}")
                return
            }
        }
    }

    /**
     * 将蓝鲸部门转为下面结构，
     * 1. 平铺所有部门，
     * 2. 每个部门添加parentDepartmentIds，表示该部门的所有父部门id
     * 3. 每个部门添加children，该部门下所有的子部门
     * [
     *      {
     *          "id" : "5",
     *          "name" : "分享",
     *          "order" : 2,
     *          "parent" : 2,
     *          "level" : 2,
     *          "has_children" : true,
     *          "permission" : false,
     *          "children" : [ {
     *              "id" : "6",
     *              "name" : "工作台",
     *              "order" : 2,
     *              "parent" : 5,
     *              "level" : 3,
     *              "has_children" : false,
     *              "permission" : false,
     *               "children" : null,
     *              "parentDepartmentIds" : [ "1", "2", "5" ]
     *          } ],
     *          "parentDepartmentIds" : [ "1", "2" ]
     *      }
     * ]
     */
    fun fetchBkDepartment() {
        val departments = bkClient.listDepartments("admin")?.sortedBy { it.level } ?: return
        val list = mutableListOf<BkChildrenDepartment>()
        val firstLevel = departments.first().level
        departments.onEach {
            if (it.level == firstLevel) {
                list.add(it)
            } else {
                addToParentDepartment(list, it)
            }
        }
        allBkDepartment = departments
    }

    fun addToParentDepartment(list: MutableList<BkChildrenDepartment>, bkChildrenDepartment: BkChildrenDepartment) {
        for (root in list) {
            if (root.id == bkChildrenDepartment.parent.toString()) {
                val parentDepartmentIds = mutableListOf<String>()
                if (!root.parentDepartmentIds.isNullOrEmpty()) parentDepartmentIds.addAll(root.parentDepartmentIds)
                parentDepartmentIds.add(root.id)
                bkChildrenDepartment.parentDepartmentIds = parentDepartmentIds
                val children = mutableListOf<BkChildrenDepartment>()
                if (!root.children.isNullOrEmpty()) children.addAll(root.children)
                children.add(bkChildrenDepartment)
                root.children = children
            } else if (!root.children.isNullOrEmpty()) {
                addToParentDepartment(root.children, bkChildrenDepartment)
            }
        }
    }

    /**
     * 判断childId是否是parentId的子部门
     */
    fun isParentDepartment(parentId: String, childId: String): Boolean {
        if (parentId == childId) return true
        val child = allBkDepartment.find { it.id == childId } ?: return false
        if (child.parentDepartmentIds.isNullOrEmpty()) return false
        return child.parentDepartmentIds.contains(parentId)
    }

    fun isParentDepartment(parentId: List<String>, childId: String): Boolean {
        if (parentId.contains(childId)) return true
        val child = allBkDepartment.find { it.id == childId } ?: return false
        if (child.parentDepartmentIds.isNullOrEmpty()) return false
        return child.parentDepartmentIds.any { parentId.contains(it) }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BkDepartmentCache::class.java)
    }
}

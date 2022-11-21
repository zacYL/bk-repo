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

    @Scheduled(fixedDelay = 15 * 60 * 1000L)
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

    fun fetchBkDepartment() {
        val departments = bkClient.listDepartments("admin")?.sortedBy { it.level }?: return
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

    companion object {
        private val logger = LoggerFactory.getLogger(BkDepartmentCache::class.java)
    }
}
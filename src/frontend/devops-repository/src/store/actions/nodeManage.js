import Vue from 'vue'

const prefix = 'replication/api'

export default {
    // 查询集群节点
    getClusterList ({ commit }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/cluster/list`
        ).then(clusterList => {
            commit('SET_CLUSTER_LIST', clusterList)
        })
    },
    checkNodeName (_, { name }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/cluster/exist`,
            {
                params: {
                    name
                }
            }
        )
    },
    // 创建集群节点
    createCluster (_, { body }) {
        return Vue.prototype.$ajax.post(
            `${prefix}/cluster/create`,
            body
        )
    },
    // 删除集群节点
    deleteCluster (_, { id }) {
        return Vue.prototype.$ajax.delete(
            `${prefix}/cluster/delete/${id}`
        )
    },
    // 查询分发计划
    getPlanList (_, { projectId, name, enabled, lastExecutionStatus, sortType, sortDirection, current = 1, limit = 10 }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/task/page/${projectId}`,
            {
                params: {
                    name,
                    enabled,
                    sortType,
                    sortDirection,
                    lastExecutionStatus,
                    pageNumber: current,
                    pageSize: limit
                }
            }
        )
    },
    // 创建分发计划
    createPlan (_, { projectId, body }) {
        return Vue.prototype.$ajax.post(
            `${prefix}/task/${projectId}/create`,
            body
        )
    },
    // 编辑分发计划
    updatePlan (_, { projectId, body }) {
        return Vue.prototype.$ajax.post(
            `${prefix}/task/${projectId}/update`,
            body
        )
    },
    // 启用/停用计划
    changeEnabled (_, { projectId, key }) {
        return Vue.prototype.$ajax.post(
            `${prefix}/task/toggle/status/${projectId}/${key}`
        )
    },
    // 执行计划
    executePlan (_, { projectId, key }) {
        return Vue.prototype.$ajax.post(
            `${prefix}/task/execute/${projectId}/${key}`
        )
    },
    // 复制计划
    copyPlan (_, { projectId, body }) {
        return Vue.prototype.$ajax.post(
            `${prefix}/task/${projectId}/copy`,
            body
        )
    },
    // 计划详情
    getPlanDetail (_, { projectId, key }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/task/detail/${projectId}/${key}`
        )
    },
    // 删除计划
    deletePlan (_, { projectId, key }) {
        return Vue.prototype.$ajax.delete(
            `${prefix}/task/delete/${projectId}/${key}`
        )
    },
    // 计划执行日志
    getPlanLogList (_, { key, status, projectId, current = 1, limit = 10 }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/task/record/page/${projectId}/${key}`,
            {
                params: {
                    pageNumber: current,
                    pageSize: limit,
                    status
                }
            }
        )
    },
    // 计划执行日志
    getPlanLogDetail (_, { projectId, id }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/task/record/${projectId}/${id}`
        )
    },
    // 计划执行日志制品详情
    getPlanLogPackageList (_, { projectId, id, status, artifactName, repoName, clusterName, current = 1, limit = 10 }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/task/record/detail/page/${projectId}/${id}`,
            {
                params: {
                    pageNumber: current,
                    pageSize: limit,
                    status: status || undefined,
                    artifactName: artifactName || undefined,
                    repoName: repoName || undefined,
                    clusterName: clusterName || undefined
                }
            }
        )
    },
    /**
     *  根据 recordId 查询任务执行日志详情总览，显示同步数量(同步总次数、成功、失败、冲突次数)
     * @param {id} 任务执行日志唯一id
     * @returns
     */
    getPlanLogDetailOverview (_, { projectId, id }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/task/record/detail/overview/${projectId}/${id}`
        )
    },
    /**
     *  获取当前用户有权限的操作集合
     * @param {projectId} 项目id
     * @returns
     */
    getPlanOperationPermission (_, { projectId }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/permission/${projectId}/query`
        )
    }
}

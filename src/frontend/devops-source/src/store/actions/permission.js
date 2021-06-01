import Vue from 'vue'

const authPrefix = 'auth/api'

export default {
    // 分页查询用户列表
    getUserList (_, { current, limit, admin }) {
        return Vue.prototype.$ajax.get(
            `${authPrefix}/user/page/${current}/${limit}`,
            {
                params: {
                    admin
                }
            }
        )
    },
    // 批量 添加/删除 管理员
    setAdmins (_, { admin, body }) {
        return Vue.prototype.$ajax.put(
            `${authPrefix}/user/admin/batch/${admin}`,
            body
        )
    },
    // 审计日志
    getAuditList (_, { startTime, endTime, operator, current, limit }) {
        return Vue.prototype.$ajax.get(
            `repository/api/operate/log/page`,
            {
                params: {
                    pageNumber: current,
                    pageSize: limit,
                    startTime,
                    endTime,
                    operator
                }
            }
        )
    },
    // 包搜索-仓库数量
    searchRepoList (_, { repoType, packageName }) {
        return Vue.prototype.$ajax.get(
            `repository/api/package/search/overview`,
            {
                params: {
                    projectId: PROJECT_ID,
                    repoType,
                    packageName
                }
            }
        )
    }
}

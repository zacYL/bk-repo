import Vue from 'vue'

const authPrefix = 'auth/api'

export default {
    getUserInfo ({ commit }, { userId }) {
        return Vue.prototype.$ajax.get(
            `${authPrefix}/user/userinfo/${userId}`
        ).then(res => {
            res && commit('SET_USER_INFO', {
                ...res,
                username: res.userId
            })
        })
    },
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
    },
    // 仓库权限列表
    getPermissionUnits (_, { repoName }) {
        return Vue.prototype.$ajax.get(
            `${authPrefix}/bksoftware/unit/${repoName}`
        )
    },
    // 添加仓库权限
    addPermissionUnits (_, { repoName, unitType, push, body }) {
        return Vue.prototype.$ajax.post(
            `${authPrefix}/bksoftware/unit/${repoName}`,
            body,
            {
                params: {
                    unitType,
                    push
                }
            }
        )
    },
    // 修改仓库权限
    editPermissionUnits (_, { repoName, unitType, push, body }) {
        return Vue.prototype.$ajax.put(
            `${authPrefix}/bksoftware/unit/permission/update/${repoName}`,
            body,
            {
                params: {
                    unitType,
                    push
                }
            }
        )
    },
    // 删除仓库权限
    deletePermissionUnits (_, { repoName, body }) {
        return Vue.prototype.$ajax.delete(
            `${authPrefix}/bksoftware/unit/${repoName}`,
            {
                data: body
            }
        )
    },
    // 查询所有部门
    getRepoDepartmentList (_, { departmentId }) {
        return Vue.prototype.$ajax.get(
            `${authPrefix}/department/list`,
            {
                params: {
                    departmentId
                }
            }
        )
    },
    // 通过部门id查询部门详情
    getRepoDepartmentDetail (_, { body }) {
        return Vue.prototype.$ajax.post(
            `${authPrefix}/department/listByIds`,
            body
        )
    }
}

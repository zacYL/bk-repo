import Vue from 'vue'

export default {
    state: {
        license: {
            // STANDARD: 标准版
            // ENTERPRISE: 企业版
            versionType: 'STANDARD'
        },
        operationPermission: [], // 当前用户在当前项目中的操作权限
        currentRepositoryDataPermission: [] // 当前用户在当前项目下的当前仓库的数据权限
    },
    getters: {
        isEnterprise (state) {
            return state.license.versionType === 'ENTERPRISE'
        }
    },
    mutations: {
        UPDATE_LICENSE (state, data) {
            state.license = data
        },
        GET_OPERATION_PERMISSION (state, data) {
            state.operationPermission = data
        },
        GET_CURRENT_REPOSITORY_DATA_PERMISSION (state, data) {
            state.currentRepositoryDataPermission = data
        }
    },
    actions: {
        // 查询仓库级别权限
        /**
         * 查询当前登录用户在当前项目下的操作权限(目前仅限于制品仓库菜单中的 创建仓库、设置仓库、删除仓库权限)
         * @param {*} param0
         * @param {*} param1
         * @returns
         */
        getOperationPermission ({ commit }, { projectId }) {
            Vue.prototype.$ajax.get(
                '/auth/api/permission/list/indevops',
                { params: { projectId } }
            ).then((res) => {
                commit('GET_OPERATION_PERMISSION', res)
            })
        },
        /**
         * 查询当前登录用户在当前项目下当前仓库的数据权限(上传制品、编辑制品、共享制品、禁用制品、锁定制品、删除制品)
         * @param {*} param0
         * @param {*} param1
         * @returns
         */
        getCurrentRepositoryDataPermission ({ commit }, { projectId, repoName }) {
            Vue.prototype.$ajax.get(
                '/auth/api/permission/list/indevops',
                { params: { projectId, repoName } }
            ).then((res) => {
                commit('GET_CURRENT_REPOSITORY_DATA_PERMISSION', res)
            })
        },
        // 请求文件夹下的子文件夹
        // override
        getFolderList ({ commit }, { projectId, repoName, roadMap, fullPath = '', isPipeline = false }) {
            return Vue.prototype.$ajax.post(
                'repository/api/node/search',
                {
                    page: {
                        pageNumber: 1,
                        pageSize: 10000
                    },
                    sort: {
                        properties: ['lastModifiedDate'],
                        direction: 'DESC'
                    },
                    rule: {
                        rules: [
                            {
                                field: 'projectId',
                                value: projectId,
                                operation: 'EQ'
                            },
                            {
                                field: 'repoName',
                                value: repoName,
                                operation: 'EQ'
                            },
                            {
                                field: 'path',
                                value: `${fullPath === '/' ? '' : fullPath}/`,
                                operation: 'EQ'
                            },
                            {
                                field: 'folder',
                                value: true,
                                operation: 'EQ'
                            }
                        ],
                        relation: 'AND'
                    }
                }
            )
        },
        // 仓库内自定义查询
        // override
        getArtifactoryList (_, { projectId, repoName, name, fullPath, current, limit, sortType = 'lastModifiedDate', sortDirection = 'DESC', searchFlag }) {
            return Vue.prototype.$ajax.post(
                'repository/api/node/search',
                {
                    page: {
                        pageNumber: current,
                        pageSize: limit
                    },
                    sort: {
                        properties: ['folder', sortType],
                        direction: sortDirection
                    },
                    rule: {
                        rules: [
                            {
                                field: 'projectId',
                                value: projectId,
                                operation: 'EQ'
                            },
                            {
                                field: 'repoName',
                                value: repoName,
                                operation: 'EQ'
                            },
                            {
                                field: 'path',
                                value: `${fullPath === '/' ? '' : fullPath}/`,
                                operation: searchFlag ? 'PREFIX' : 'EQ'
                            },
                            ...(name
                                ? [
                                    {
                                        field: 'name',
                                        value: `\*${name}\*`,
                                        operation: 'MATCH'
                                    }
                                ]
                                : []
                            )
                        ],
                        relation: 'AND'
                    }
                }
            )
        },
        // 获取许可信息
        getModuleInfo ({ commit }) {
            Vue.prototype.$ajax.get('repository/api/license').then(data => {
                commit('UPDATE_LICENSE', data)
            })
        }
    }
}

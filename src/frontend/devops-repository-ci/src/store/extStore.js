import Vue from 'vue'

export default {
    state: {
        license: {
            // STANDARD: 标准版
            // ENTERPRISE: 企业版
            versionType: 'STANDARD'
        }
    },
    getters: {
        isEnterprise (state) {
            return state.license.versionType === 'ENTERPRISE'
        }
    },
    mutations: {
        UPDATE_LICENSE (state, data) {
            state.license = data
        }
    },
    actions: {
        // 查询仓库级别权限
        getRepoPermission (_, { projectId, action, repoName }) {
            return Vue.prototype.$ajax.get(
                `/repository/api/permission/${projectId}/${action}`,
                { params: { repoName } }
            )
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
                        properties: [isPipeline ? 'lastModifiedDate' : 'name'],
                        direction: isPipeline ? 'DESC' : 'ASC'
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

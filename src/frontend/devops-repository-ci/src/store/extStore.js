import Vue from 'vue'

export default {
    state: {},
    getters: {},
    mutations: {},
    actions: {
        // 查询仓库级别权限
        getRepoPermission (_, { projectId, action, repoName }) {
            return Vue.prototype.$ajax.get(
                `/repository/api/permission/${projectId}/${action}`,
                { params: { repoName } }
            )
        },
        // 查询所有角色
        getRepoRoleList (_, { projectId, repoName }) {
            return Vue.prototype.$ajax.get(
                `auth/api/role/list`,
                {
                    params: {
                        projectId,
                        repoName
                    }
                }
            )
        },
        // 查询所有动作
        getRepoActions (_, { projectId, repoName }) {
            return Vue.prototype.$ajax.get(
                `auth/api/action/${projectId}/${repoName}/optional`
            )
        },
        // 请求文件夹下的子文件夹
        // override
        getFolderList ({ commit }, { projectId, repoName, roadMap, fullPath = '', isPipeline = false }) {
            return Vue.prototype.$ajax.post(
                `repository/api/node/query`,
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
            ).then(({ records }) => {
                commit('UPDATE_TREE', {
                    roadMap,
                    list: records.map((v, index) => ({
                        ...v,
                        roadMap: `${roadMap},${index}`
                    }))
                })
            })
        },
        // 仓库内自定义查询
        // override
        getArtifactoryList (_, { projectId, repoName, name, fullPath, current, limit, sortType = 'lastModifiedDate' }) {
            return Vue.prototype.$ajax.post(
                `repository/api/node/query`,
                {
                    page: {
                        pageNumber: current,
                        pageSize: limit
                    },
                    sort: {
                        properties: ['folder', sortType],
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
                            ...(name ? [
                                {
                                    field: 'name',
                                    value: `\*${name}\*`,
                                    operation: 'MATCH'
                                }
                            ] : [
                                {
                                    field: 'path',
                                    value: `${fullPath === '/' ? '' : fullPath}/`,
                                    operation: 'EQ'
                                }
                            ])
                        ],
                        relation: 'AND'
                    }
                }
            )
        },
        // 分享文件
        // override
        shareArtifactory (_, { projectId, repoName, body, fullPath = '' }) {
            return Vue.prototype.$ajax.post(
                `repository/api/share/${projectId}/${repoName}/${encodeURIComponent(fullPath)}`,
                body
            )
        }
    }
}

import Vue from 'vue'

export default {
    state: {
        permission: {
            write: false,
            edit: false,
            delete: false
        }
    },
    getters: {},
    mutations: {},
    actions: {
        // 分页查询仓库列表
        // override
        getRepoList (_, { projectId, current, limit, name, type, category }) {
            return Vue.prototype.$ajax.get(
                `repository/api/software/repo/page/${current}/${limit}`,
                {
                    params: {
                        projectId: projectId || undefined,
                        name: name || undefined,
                        type: type || undefined,
                        category: category || undefined
                    }
                }
            )
        },
        // 包搜索-仓库数量
        // override
        searchRepoList (_, { projectId, repoType, packageName }) {
            const isGeneric = repoType === 'generic'
            return Vue.prototype.$ajax.get(
                `repository/api/software/${isGeneric ? 'node' : 'package'}/search/overview`,
                {
                    params: {
                        projectId,
                        repoType: repoType.toUpperCase(),
                        [isGeneric ? 'name' : 'packageName']: `*${packageName}*`
                    }
                }
            )
        },
        // 跨仓库搜索
        // override
        searchPackageList (_, { projectId, repoType, repoName, packageName, property = 'name', direction = 'ASC', current = 1, limit = 20, version = '', metadataList = [], sha256 = '', md5 = '', artifactList = [] }) {
            const isGeneric = repoType === 'generic'
            return Vue.prototype.$ajax.post(
                `repository/api/software/${isGeneric ? 'node/search' : 'package/search'}`,
                {
                    page: {
                        pageNumber: current,
                        pageSize: limit
                    },
                    sort: {
                        properties: [property],
                        direction
                    },
                    rule: {
                        rules: [
                            ...(projectId
                                ? [{
                                    field: 'projectId',
                                    value: projectId,
                                    operation: 'EQ'
                                }]
                                : []),
                            ...(artifactList.length > 0
                                ? [
                                    {
                                        field: 'repoName',
                                        value: artifactList,
                                        operation: 'IN'
                                    }
                                ]
                                : []),
                            ...(repoType
                                ? [{
                                    field: 'repoType',
                                    value: repoType.toUpperCase(),
                                    operation: 'EQ'
                                }]
                                : []),
                            ...(repoName
                                ? [{
                                    field: 'repoName',
                                    value: repoName,
                                    operation: 'EQ'
                                }]
                                : []),
                            ...(packageName
                                ? [{
                                    field: 'name',
                                    value: `*${packageName}*`,
                                    operation: 'MATCH_I'
                                }]
                                : []),
                            ...(version
                                ? [{
                                    field: 'version',
                                    value: `*${version}*`,
                                    operation: 'MATCH_I'
                                }]
                                : []),
                            ...(metadataList
                                ? metadataList?.map((item) => {
                                    // 不做下面的判断会导致接口传参添加了field: 'metadata.' 的对象，会导致搜索结果为空
                                    if (item.key && item.value) {
                                        return {
                                            field: `metadata.${item.key}`,
                                            value: item.value,
                                            operation: 'EQ'
                                        }
                                    } else {
                                        return ''
                                    }
                                }).filter(Boolean)
                                : []),
                            ...(md5
                                ? [{
                                    field: 'md5',
                                    value: `${md5}`,
                                    operation: 'EQ'
                                }]
                                : []),
                            ...(sha256
                                ? [{
                                    field: 'sha256',
                                    value: `${sha256}`,
                                    operation: 'EQ'
                                }]
                                : []),
                            ...(isGeneric
                                ? [{
                                    field: 'folder',
                                    value: false,
                                    operation: 'EQ'
                                }]
                                : [])
                            
                        ],
                        relation: 'AND'
                    }
                }
            )
        },
        // 查询仓库列表，不分页
        getRepoListAll ({ commit }, { type }) {
            return Vue.prototype.$ajax.get(
                'repository/api/software/repo/list',
                {
                    params: {
                        type: type || ''
                    }
                }
            )
        }
    }
}

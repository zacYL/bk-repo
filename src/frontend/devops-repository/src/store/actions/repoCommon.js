import Vue from 'vue'

const prefix = 'repository/api'

export default {
    // 分页查询包列表
    getPackageList (_, { projectId, repoName, packageName, current = 1, limit = 10 }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/package/page/${projectId}/${repoName}`,
            {
                params: {
                    pageNumber: current,
                    pageSize: limit,
                    packageName
                }
            }
        )
    },
    // 删除包
    deletePackage (_, { projectId, repoType, repoName, packageKey }) {
        return Vue.prototype.$ajax.delete(
            `${repoType}/ext/package/delete/${projectId}/${repoName}`,
            {
                params: {
                    packageKey
                }
            }
        )
    },
    // 查询包信息
    getPackageInfo (_, { projectId, repoName, packageKey }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/package/info/${projectId}/${repoName}`,
            {
                params: {
                    packageKey
                }
            }
        )
    },
    // 查询包版本列表
    getVersionList (_, { projectId, repoName, packageKey, version, current = 1, limit = 10 }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/version/page/${projectId}/${repoName}`,
            {
                params: {
                    pageNumber: current,
                    pageSize: limit,
                    packageKey,
                    version
                }
            }
        )
    },
    // 删除包版本
    deleteVersion (_, { projectId, repoType, repoName, packageKey, version }) {
        return Vue.prototype.$ajax.delete(
            `${repoType}/ext/version/delete/${projectId}/${repoName}`,
            {
                params: {
                    packageKey,
                    version
                }
            }
        )
    },
    // 查询包版本详情
    getVersionDetail (_, { projectId, repoType, repoName, packageKey, version }) {
        return Vue.prototype.$ajax.get(
            `${repoType}/ext/version/detail/${projectId}/${repoName}`,
            {
                params: {
                    packageKey,
                    version
                }
            }
        )
    },
    // 包搜索-仓库数量
    searchRepoList (_, { projectId, repoType, packageName }) {
        const isGeneric = repoType === 'generic'
        return Vue.prototype.$ajax.get(
            `${prefix}/${isGeneric ? 'node' : 'package'}/search/overview`,
            {
                params: {
                    projectId,
                    repoType: repoType.toUpperCase(),
                    [isGeneric ? 'name' : 'packageName']: `*${packageName}*`,
                    ...(MODE_CONFIG === 'ci' && isGeneric
                        ? {
                            exRepo: 'report,log'
                        }
                        : {})
                }
            }
        )
    },
    // 跨仓库搜索
    searchPackageList (_, { projectId, repoType, repoName, packageName, property = 'name', direction = 'ASC', current = 1, limit = 20, extRules = [] }) {
        const isGeneric = repoType === 'generic'
        return Vue.prototype.$ajax.post(
            `${prefix}/${isGeneric ? 'node/query' : 'package/search'}`,
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
                            : [
                                ...(MODE_CONFIG === 'ci' && isGeneric
                                    ? [{
                                        field: 'repoName',
                                        value: ['report', 'log'],
                                        operation: 'NIN'
                                    }]
                                    : [])
                            ]),
                        ...(packageName
                            ? [{
                                field: 'name',
                                value: `*${packageName}*`,
                                operation: 'MATCH'
                            }]
                            : []),
                        ...(isGeneric
                            ? [{
                                field: 'folder',
                                value: false,
                                operation: 'EQ'
                            }]
                            : []),
                        ...extRules
                        
                    ],
                    relation: 'AND'
                }
            }
        )
    },
    // 获取相应服务的域名
    getDomain ({ state, commit }, repoType) {
        const urlMap = {
            docker: 'docker/ext/addr',
            npm: 'npm/ext/address'
        }
        if (!urlMap[repoType] || state.domain[repoType]) return
        Vue.prototype.$ajax.get(
            urlMap[repoType]
        ).then(res => {
            commit('SET_DOMAIN', {
                type: repoType,
                domain: res.domain || res || `${location.origin}/${repoType}`
            })
        })
    },
    // 制品晋级
    changeStageTag (_, { projectId, repoName, packageKey, version, tag }) {
        return Vue.prototype.$ajax.post(
            `${prefix}/stage/upgrade/${projectId}/${repoName}`,
            null,
            {
                params: {
                    packageKey,
                    version,
                    tag
                }
            }
        )
    },
    initArtifactTypeList ({ commit }) {
        return Vue.prototype.$ajax.get(`${prefix}/remote/whitelist/optional/type`)
            .then(res => {
                commit('SET_ARTIFACT_LIST', res)
            })
    },
    // 查询白名单列表
    getWhitelist (_, { repositoryType, packageKey, version, pageNumber, pageSize }) {
        return Vue.prototype.$ajax.get(`${prefix}/remote/whitelist/page?type=${repositoryType}&packageKey=${packageKey}&version=${version}&pageNumber=${pageNumber}&pageSize=${pageSize}&regex=true`)
    },
    addWhiteList (_, { packageKey, versions, type }) {
        return Vue.prototype.$ajax.put(`${prefix}/remote/whitelist`, { packageKey, versions, type })
    },
    editWhiteList (_, { id, packageKey, versions, type }) {
        return Vue.prototype.$ajax.post(`${prefix}/remote/whitelist/${id}`, { packageKey, versions, type })
    },
    delWhiteList (_, { id }) {
        return Vue.prototype.$ajax.delete(`${prefix}/remote/whitelist/${id}`)
    },
    delProjectByName (_, { name }) {
        return Vue.prototype.$ajax.delete(`${prefix}/project/delete/${name}?confirmName=${name}`)
    }
}

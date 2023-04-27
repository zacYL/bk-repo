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
    // 虚拟仓库时需要添加仓库来源字段srcRepo，不然后端无法知道当前制品需要从当前的虚拟仓库中的哪个仓库中获取
    // (同一个虚拟仓库中不同仓库中的制品可能重名)
    getVersionList (_, { projectId, repoName, packageKey, version, srcRepo = undefined, current = 1, limit = 10 }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/version/page/${projectId}/${repoName}`,
            {
                params: {
                    pageNumber: current,
                    pageSize: limit,
                    packageKey,
                    version,
                    srcRepo: srcRepo || undefined
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
                            exRepo: 'log'
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
                                        value: ['log'],
                                        operation: 'NIN'
                                    }]
                                    : [])
                            ]),
                        ...(packageName
                            ? [{
                                field: 'name',
                                value: `*${packageName}*`,
                                operation: 'MATCH_I'
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
    getWhitelist (_, { type, packageKey, version, pageNumber, pageSize, regex = true }) {
        return Vue.prototype.$ajax.get(`${prefix}/remote/whitelist/page`, {
            params: { type, packageKey, version, pageNumber, pageSize, regex }
        })
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
    },
    // 仓库类型是否开启拦截清单
    getWhiteListSwitchList ({ state, commit }) {
        return Vue.prototype.$ajax.get(`${prefix}/remote/whitelist/switch/list`)
    },
    // 修改仓库类型拦截状态
    updateWhiteListSwitchList ({ state, commit }, { RepositoryType }) {
        return Vue.prototype.$ajax.post(`${prefix}/remote/whitelist/switch/${RepositoryType}`)
    },
    // maven仓库获取正向依赖
    getCorrectDependencies (_, { projectId, repoName, packageKey, version, pageNumber, pageSize }) {
        return Vue.prototype.$ajax.get(`/maven/ext/dependencies/${projectId}/${repoName}`, {
            params: {
                packageKey,
                version,
                pageNumber,
                pageSize
            }
        })
    },
    // maven仓库获取插件
    getCorrectPlugins (_, { projectId, repoName, packageKey, version, pageNumber, pageSize }) {
        return Vue.prototype.$ajax.get(`/maven/ext/plugins/${projectId}/${repoName}`, {
            params: {
                packageKey,
                version,
                pageNumber,
                pageSize
            }
        })
    },
    // maven仓库获取反向依赖
    getReverseDependencies (_, { projectId, repoName, packageKey, version, pageNumber, pageSize }) {
        return Vue.prototype.$ajax.get(`/maven/ext/dependencies/reverse/${projectId}/${repoName}`, {
            params: {
                packageKey,
                version,
                pageNumber,
                pageSize
            }
        })
    },
    /**
     * maven上传制品解析成功后点击取消，实际上制品并没有上传成功，此时需要删除依赖目录中展示的jar包
     * @param {*} _
     * @param {*} projectId 项目Id
     * @param {*} repoName 仓库名
     * @param {*} groupId 后端解析上传的文件groupId
     * @param {*} artifactId 后端解析上传的文件artifactId
     * @param {*} version 后端解析上传的文件版本
     * @param {*} artifactName 上传文件名
     * @returns
     */
    deleteErrorPackage (_, { projectId, repoName, groupId, artifactId, version, artifactName }) {
        return Vue.prototype.$ajax.delete(`${prefix}/node/delete/${projectId}/${repoName}/${groupId}/${artifactId}/${version}/${artifactName}`)
    }
}

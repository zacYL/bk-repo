import Vue from 'vue'

const prefix = 'repository/api'

export default {
    // 分页查询包列表
    getPackageList (_, { repoName, packageName, current = 1, limit = 10 }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/package/page/${PROJECT_ID}/${repoName}`,
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
    deletePackage (_, { repoType, repoName, packageKey }) {
        return Vue.prototype.$ajax.delete(
            `${repoType}/ext/package/delete/${PROJECT_ID}/${repoName}`,
            {
                params: {
                    packageKey
                }
            }
        )
    },
    // 查询包信息
    getPackageInfo (_, { repoName, packageKey }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/package/info/${PROJECT_ID}/${repoName}`,
            {
                params: {
                    packageKey
                }
            }
        )
    },
    // 查询包版本列表
    getVersionList (_, { repoName, packageKey, version, current = 1, limit = 10 }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/version/page/${PROJECT_ID}/${repoName}`,
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
    deleteVersion (_, { repoType, repoName, packageKey, version }) {
        return Vue.prototype.$ajax.delete(
            `${repoType}/ext/version/delete/${PROJECT_ID}/${repoName}`,
            {
                params: {
                    packageKey,
                    version
                }
            }
        )
    },
    // 查询包版本详情
    getVersionDetail (_, { repoType, repoName, packageKey, version }) {
        return Vue.prototype.$ajax.get(
            `${repoType}/ext/version/detail/${PROJECT_ID}/${repoName}`,
            {
                params: {
                    packageKey,
                    version
                }
            }
        )
    },
    // npm查询被依赖列表
    getNpmDependents (_, { repoName, packageKey, current = 1 }) {
        return Vue.prototype.$ajax.get(
            `npm/ext/dependent/page/${PROJECT_ID}/${repoName}`,
            {
                params: {
                    pageNumber: current,
                    pageSize: 20,
                    packageKey
                }
            }
        )
    },
    // 跨仓库搜索包
    searchPackageList (_, { repoType, repoName, packageName, current = 1, limit = 20 }) {
        return Vue.prototype.$ajax.post(
            `${prefix}/package/search`,
            {
                page: {
                    pageNumber: current,
                    pageSize: limit
                },
                sort: {
                    properties: ['name'],
                    direction: 'ASC'
                },
                rule: {
                    rules: [
                        {
                            field: 'projectId',
                            value: PROJECT_ID,
                            operation: 'EQ'
                        },
                        {
                            field: 'repoType',
                            value: repoType.toUpperCase(),
                            operation: 'EQ'
                        },
                        ...(repoName ? [{
                            field: 'repoName',
                            value: repoName,
                            operation: 'EQ'
                        }] : []),
                        {
                            field: 'name',
                            value: '*' + packageName + '*',
                            operation: 'MATCH'
                        }
                    ],
                    relation: 'AND'
                }
            }
        )
    },
    // 获取docker域名
    getDockerDomain ({ commit }) {
        Vue.prototype.$ajax.get(
            `docker/ext/addr`
        ).then(data => {
            commit('SET_DOCKER_DOMAIN', data)
        })
    },

    // 制品晋级
    changeStageTag (_, { repoName, packageKey, version, tag }) {
        return Vue.prototype.$ajax.post(
            `${prefix}/stage/upgrade/${PROJECT_ID}/${repoName}`,
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
    // 添加元数据
    addPackageMetadata (_, { projectId, repoName, body }) {
        return Vue.prototype.$ajax.post(
            `${prefix}/metadata/package/${projectId}/${repoName}`,
            body
        )
    }
}

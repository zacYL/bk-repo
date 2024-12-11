import Vue from 'vue'
import axios from 'axios'

const prefix = 'repository/api'

const getPackageParams = ({ projectId, repoType, repoName, packageName, property = 'name', direction = 'ASC', current = 1, limit = 20, extRules = [], version = '', metadataList = [], sha256 = '', md5 = '', artifactList = [] }) => {
    const isGeneric = repoType === 'generic'
    return {
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
                ...(repoName
                    ? [{
                        field: 'repoName',
                        value: repoName,
                        operation: 'EQ'
                    }]
                    : [
                        // 因为在仓库数量太大(例如超过1000)，package/search接口在存在repoType时响应太慢，所以只有在必须通过repoType确认仓库时才需要
                        // 依赖源仓库进入仓库的包列表页不需要传此参数，目前只有制品搜索需要传repoType，其他的都可以通过projectId和repoName唯一确认
                        ...(repoType
                            ? [{
                                field: 'repoType',
                                value: repoType.toUpperCase(),
                                operation: 'EQ'
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
                ...(version
                    ? [{
                        field: 'version',
                        value: `${version}`,
                        operation: 'EQ'
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
                    : []),
                ...extRules
            ],
            relation: 'AND'
        }
    }
}

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
    getVersionList (_, { projectId, repoName, packageKey, version, current = 1, limit = 10, sortProperty = '' }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/version/page/${projectId}/${repoName}`,
            {
                params: {
                    pageNumber: current,
                    pageSize: limit,
                    packageKey,
                    version,
                    ...sortProperty
                        ? {
                            sortProperty
                        }
                        : {}
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
    packageListExport (_, data) {
        const url = `${prefix}/package/export`
        return axios({
            baseURL: `${location.origin}/web`,
            url,
            method: 'POST',
            data: getPackageParams(data),
            // 注意，此处需要设置下载的文件的返回类型为二进制，即 blob
            responseType: 'blob',
            withCredentials: true,
            xsrfCookieName: (MODE_CONFIG === 'ci' || MODE_CONFIG === 'saas') ? 'bk_token' : 'bkrepo_ticket', // 注入csrfToken
            xsrfHeaderName: 'X-CSRFToken', // 注入csrfToken
            headers: { 'Accept-Language': this.currentLanguage }
        })
    },
    // 跨仓库搜索
    searchPackageList (_, data) {
        const isGeneric = data.repoType === 'generic'
        return Vue.prototype.$ajax.post(
            `${prefix}/${isGeneric ? 'node/queryWithoutCount' : 'package/search'}`,
            getPackageParams(data)
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
     * 包版本添加元数据
     * @param {*} _
     * @param {*} projectId 项目Id
     * @param {*} repoName 仓库名
     * @param {*} body
     * @returns
     */
    addPackageMetadata (_, { projectId, repoName, body }) {
        return Vue.prototype.$ajax.post(
            `${prefix}/metadata/package/${projectId}/${repoName}`,
            body
        )
    },
    /**
     * 包版本删除元数据
     * @param {*} _
     * @param {*} projectId 项目Id
     * @param {*} repoName 仓库名
     * @param {*} body
     * @returns
     */
    deletePackageMetadata (_, { projectId, repoName, body }) {
        return Vue.prototype.$ajax.delete(
            `${prefix}/metadata/package/${projectId}/${repoName}`,
            { data: body }
        )
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
    deleteUselessPackage (_, { projectId, repoName, fullPath }) {
        return Vue.prototype.$ajax.post(`/maven/cancel/${projectId}/${repoName}/${encodeURIComponent(fullPath)}`)
    }
}

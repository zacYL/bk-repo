import Vue from 'vue'

const prefix = 'repository/api'

export default {
    // 获取当前项目下所有依赖树的根节点
    getRepoRelyList ({ commit }, { projectId, type = '' }) {
        const url = type.length > 0 ? `${prefix}/repo/list/package/${projectId}?type=${type}` : `${prefix}/repo/list/package/${projectId}`
        return Vue.prototype.$ajax.get(url)
    },
    // 获取当前项目下所选仓库的子节点
    getSubTreeNodes (_, { projectId, repoName, fullPath, current = 1, limit = 500, sortType = 'lastModifiedDate' }) {
        return Vue.prototype.$ajax.post(`${prefix}/node/search`, {
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
                    {
                        field: 'path',
                        value: `${fullPath === '/' ? '' : fullPath}/`,
                        operation: 'EQ'
                    }
                ],
                relation: 'AND'
            }
        })
    },
    // 下载文件
    getFileBlobDetail ({ commit }, { projectId, repoName, fullPath }) {
        return Vue.prototype.$ajax.get(`${prefix}/list/${projectId}/${repoName}/${fullPath}`)
    },
    // 获取仓库详情数据
    getRepoInfoDetail ({ commit }, { projectId, repoName }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/repo/info/${projectId}/${repoName}`
        )
    },
    // 分页， 依赖目录页面搜索功能，通过文件或文件夹名称正则匹配符合条件的文件或文件夹，需要排除二进制仓库
    getTableListByName ({ commit }, { projectId, name, repoType = ['DOCKER', 'MAVEN', 'PYPI', 'NPM', 'HELM', 'RDS', 'COMPOSER', 'RPM', 'NUGET', 'GIT', 'OCI', 'CONAN'], current = 1, limit = 20, sortType = 'lastModifiedDate' }) {
        return Vue.prototype.$ajax.post(`${prefix}/node/search`, {
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
                        field: 'repoType',
                        value: repoType,
                        operation: 'IN'
                    },
                    {
                        field: 'name',
                        value: `*${name}*`,
                        operation: 'MATCH'
                    }
                ],
                relation: 'AND'
            }
        })
    }

}

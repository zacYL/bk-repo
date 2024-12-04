import Vue from 'vue'
import cookie from 'js-cookie'

import repoGeneric from './repoGeneric'
import repoCommon from './repoCommon'
import token from './token'
import permission from './permission'
import nodeManage from './nodeManage'
import networkConfig from './networkConfig'
import project from './project'
import scan from './scan'
import repoCatalog from './repoCatalog'
import blackWhiteList from './blackWhiteList'

const prefix = 'repository/api'
const auth = 'auth/api'

export default {
    ...repoGeneric,
    ...repoCommon,
    ...token,
    ...permission,
    ...nodeManage,
    ...networkConfig,
    ...project,
    ...scan,
    ...repoCatalog,
    ...blackWhiteList,

    /*
        创建仓库
        body: {
            "projectId": "test",
            "name": "generic-local",
            "type": "GENERIC",
            "category": "COMPOSITE",
            "public": false,
            "description": "repo description",
            "configuration": null,
            "storageCredentialsKey": null
        }
    */
    createRepo ({ dispatch }, { body }) {
        return Vue.prototype.$ajax.post(
            `${prefix}/repo/create`,
            body
        )
    },
    // 校验仓库名称
    checkRepoName (_, { projectId, name }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/repo/exist/${projectId}/${name}`
        )
    },
    // 分页查询仓库列表
    getRepoList (_, { projectId, current, limit, name, type, category }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/repo/page/${projectId}/${current}/${limit}`,
            {
                params: {
                    name: name || undefined,
                    type: type || undefined,
                    category: category || undefined
                }
            }
        )
    },
    // 获取有复制移动权限的generic仓库列表
    getGenericList (_, { projectId }) {
        return Vue.prototype.$ajax.get(`${prefix}/repo/list/${projectId}?type=GENERIC`)
    },
    // 查询仓库列表
    getRepoListAll ({ commit }, { projectId, type, searchFlag = false }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/repo/list/${projectId}`,
            {
                params: {
                    type: type || ''
                }
            }
        ).then(res => {
            return searchFlag ? res : commit('SET_REPO_LIST_ALL', res)
        })
    },
    // 查询权限仓库列表
    getReadRepoListAll ({ commit }, { projectId, type, searchFlag = false }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/repo/list/${projectId}`,
            {
                params: {
                    type: type || '',
                    actions: 'READ'

                }
            }
        ).then(res => {
            return searchFlag ? res : commit('SET_READ_REPO_LIST_ALL', res)
        })
    },
    // 查询仓库信息
    getRepoInfo (_, { projectId, repoName, repoType }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/repo/info/${projectId}/${repoName}/${repoType.toUpperCase()}`
        )
    },
    // 更新仓库信息
    updateRepoInfo (_, { projectId, name, body }) {
        return Vue.prototype.$ajax.post(
            `${prefix}/repo/update/${projectId}/${name}`,
            body
        )
    },
    // 删除仓库
    deleteRepoList ({ dispatch }, { projectId, name, forced = false }) {
        return Vue.prototype.$ajax.delete(
            `${prefix}/repo/delete/${projectId}/${name}?forced=${forced}`
        )
    },
    // 查询项目列表
    getProjectList ({ commit }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/project/list`
        ).then(res => {
            commit('SET_PROJECT_LIST', res)
        })
    },
    logout ({ commit }) {
        if (MODE_CONFIG === 'ci' || MODE_CONFIG === 'saas') {
            window.postMessage({
                action: 'toggleLoginDialog'
            }, '*')
            location.href = window.getLoginUrl()
        } else {
            cookie.remove('bkrepo_ticket')
            commit('SHOW_LOGIN_DIALOG', true)
        }
    },
    // 创建远程仓库时测试链接
    testRemoteUrl (_, { body }) {
        return Vue.prototype.$ajax.post(
            `${prefix}/repo/testremote`,
            body
        )
    },
    /**
     * @description: 创建仓库权限路径集合资源
     * @param {*} _
     * @param {*} body
     * @return {*}
     */
    repoPathCreate (_, { body }) {
        return Vue.prototype.$ajax.post(
            `${auth}/permission/resource_type/repo_path_collection/create`,
            body
        )
    },
    /**
     * @description: 删除仓库权限路径集合资源
     * @param {*} _
     * @param {*} body
     * @return {*}
     */
    repoPathDelete (_, { body }) {
        return Vue.prototype.$ajax.post(
            `${auth}/permission/resource_type/repo_path_collection/delete`,
            body
        )
    },
    /**
     * @description: 仓库权限路径集合资源列表
     * @param {*} _
     * @param {*} body
     * @return {*}
     */
    repoPathList (_, { body }) {
        return Vue.prototype.$ajax.post(
            `${auth}/permission/resource_type/repo_path_collection/list`,
            body
        )
    },
    /**
     * @description: 更新仓库权限路径集合资源
     * @param {*} _
     * @param {*} body
     * @return {*}
     */
    repoPathUpdate (_, { body }) {
        return Vue.prototype.$ajax.post(
            `${auth}/permission/resource_type/repo_path_collection/update`,
            body
        )
    }
}

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

const prefix = 'repository/api'

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
        ).then(res => ({
            ...res,
            records: MODE_CONFIG === 'ci'
                ? res.records.filter(v => v.name !== 'log')
                : res.records
        })) // 前端隐藏report仓库/log仓库
    },
    // 获取有复制移动权限的generic仓库列表
    getGenericList (_, { projectId }) {
        return Vue.prototype.$ajax.get(`${prefix}/repo/list/${projectId}?type=GENERIC&actions=WRITE&actions=READ`)
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
            // 前端隐藏 log仓库
            const backData = res.filter(v => v.name !== 'log')
            if (searchFlag) {
                return backData
            } else {
                commit('SET_REPO_LIST_ALL', backData)
            }
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
    }
}

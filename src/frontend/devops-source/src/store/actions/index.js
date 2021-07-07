import Vue from 'vue'

import repoCommon from './repoCommon'
import token from './token'
import permission from './permission'

const prefix = 'repository/api'

export default {
    ...repoCommon,
    ...token,
    ...permission,
    createRepo (_, { body }) {
        return Vue.prototype.$ajax.post(
            `${prefix}/repo/create`,
            body
        )
    },
    // 校验仓库名称
    checkRepoName (_, { name }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/repo/exist/${PROJECT_ID}/${name}`
        )
    },
    // 分页查询仓库列表
    getRepoList (_, { current, limit, name, type, usedInfo }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/repo/page/${PROJECT_ID}/${current}/${limit}`,
            {
                params: {
                    usedInfo,
                    name,
                    type
                }
            }
        )
    },
    // 查询仓库列表
    getRepoListAll ({ commit }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/repo/list/${PROJECT_ID}`
        ).then(repoList => {
            commit('SET_REPO_LIST', repoList)
        })
    },
    // 查询仓库信息
    getRepoInfo (_, { repoName, repoType }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/repo/info/${PROJECT_ID}/${repoName}/${repoType}`
        )
    },
    // 更新仓库信息
    updateRepoInfo (_, { name, body }) {
        return Vue.prototype.$ajax.post(
            `${prefix}/repo/update/${PROJECT_ID}/${name}`,
            body
        )
    },
    // 删除仓库
    deleteRepoList (_, { name, forced = false }) {
        return Vue.prototype.$ajax.delete(
            `${prefix}/repo/delete/${PROJECT_ID}/${name}?forced=${forced}`
        )
    },
    // 查询公有源列表
    getPublicProxy (_, { repoType }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/proxy-channel/list/public/${repoType}`
        )
    },
    // 查询项目列表
    getProjectList ({ commit }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/project/list`
        ).then(res => {
            commit('SET_PROJECT_LIST', res.map(v => {
                return {
                    id: v.name,
                    name: v.displayName
                }
            }))
        })
    },
    logout () {
        window.postMessage({
            action: 'toggleLoginDialog'
        }, '*')
        location.href = window.getLoginUrl()
    }
}

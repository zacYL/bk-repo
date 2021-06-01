import Vue from 'vue'

const prefix = 'auth/api'

export default {
    // 用户token列表
    getTokenList (_, { username }) {
        return Vue.prototype.$ajax.get(
            `${prefix}/user/list/token/${username}`
        )
    },
    // 新增用户token
    addToken (_, { username, name, expiredAt = '' }) {
        return Vue.prototype.$ajax.post(
            `${prefix}/user/token/${username}/${name}`,
            null,
            {
                params: {
                    projectId: PROJECT_ID,
                    expiredAt
                }
            }
        )
    },
    // 删除用户token
    deleteToken (_, { username, name }) {
        return Vue.prototype.$ajax.delete(
            `${prefix}/user/token/${username}/${name}`
        )
    }
}

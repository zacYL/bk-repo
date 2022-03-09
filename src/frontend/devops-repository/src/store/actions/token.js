import Vue from 'vue'

const prefix = 'auth/api'

export default {
    // 用户token列表
    getTokenList () {
        return Vue.prototype.$ajax.get(
            `${prefix}/user/list/token`
        )
    },
    // 新增用户token
    addToken (_, { projectId, name, expiredAt = '' }) {
        return Vue.prototype.$ajax.put(
            `${prefix}/user/token/${name}`,
            null,
            {
                params: {
                    projectId,
                    expiredAt
                }
            }
        )
    },
    // 删除用户token
    deleteToken (_, { name }) {
        return Vue.prototype.$ajax.delete(
            `${prefix}/user/token/${name}`
        )
    },
    // 登录
    getRSAKey () {
        return Vue.prototype.$ajax.get(
            `${prefix}/user/rsa`
        )
    },
    // 登录
    bkrepoLogin (_, formData) {
        return Vue.prototype.$ajax.post(
            `${prefix}/user/login`,
            formData,
            { headers: { 'Content-Type': 'multipart/form-data' } }
        )
    }
}

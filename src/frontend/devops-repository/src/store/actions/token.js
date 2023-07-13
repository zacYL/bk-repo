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
    },
    /**
     * 集成CI模式下生成个人访问令牌
     * @param {tokenName} String token名称
     * @param {tokenScope} Array token范围，此时需要写死传 ['CPack]
     * @returns
     */
    ciCreateToken (_, { tokenName, tokenScope = ['CPack'] }) {
        return Vue.prototype.$ajax.post(
            'usermanager/api/user/token/add',
            {
                tokenName,
                tokenScope
            },
            { baseURL: `${window.DEVOPS_SITE_URL}/ms/` }
        )
    }
}

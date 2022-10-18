import Vue from 'vue'

const prefix = 'repository/api'

export default {
    // 获取网络速率
    getNetworkRatelimit ({ commit }) {
        return Vue.prototype.$ajax.get(`${prefix}/config/info`)
    },
    // 保存网络速率
    saveNetworkRatelimit ({ commit }, { value }) {
        return Vue.prototype.$ajax.post(`${prefix}/config/update`, { replicationNetworkRate: value })
    }
}

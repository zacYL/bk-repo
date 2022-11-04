import Vue from 'vue'

const prefix = 'repository/api'

export default {
    // 获取网络速率
    getNetworkRatelimit ({ commit }, { type }) {
        return Vue.prototype.$ajax.get(`${prefix}/config/info?type=${type}`)
    },
    // 保存网络速率
    saveNetworkRatelimit ({ commit }, body) {
        return Vue.prototype.$ajax.post(`${prefix}/config/update`, body)
    }
}

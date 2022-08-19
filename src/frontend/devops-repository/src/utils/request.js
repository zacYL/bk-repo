import axios from 'axios'
import Vue from 'vue'

const request = axios.create({
    baseURL: `${location.origin}/web`,
    validateStatus: status => {
        if (status > 400) {
            console.warn(`HTTP 请求出错 status: ${status}`)
        }
        return status >= 200 && status <= 503
    },
    withCredentials: true,
    xsrfCookieName: (MODE_CONFIG === 'ci' || MODE_CONFIG === 'saas') ? 'bk_token' : 'bkrepo_ticket', // 注入csrfToken
    xsrfHeaderName: 'X-CSRFToken' // 注入csrfToken
})

function errorHandler (error) {
    console.log('error catch', error)
    return Promise.reject(Error('网络出现问题，请检查你的网络是否正常'))
}

request.interceptors.response.use(response => {
    const { data: { data, message, error }, status } = response

    // 用于处理仓库列表helm仓库返回数据格式无法统一，造成的基础信息显示错误问题
    if (status === 404 && error) {
        return {
            basic: {},
            metadata: []
        }
    }

    // 正常逻辑
    if (status === 200 || status === 206) {
        return data === undefined ? response.data : data
    } else if (status === 401 || status === 402) {
        if (MODE_CONFIG === 'ci' || MODE_CONFIG === 'saas') {
            window.postMessage({
                action: 'toggleLoginDialog'
            }, '*')
            location.href = window.getLoginUrl()
        } else {
            window.repositoryVue.$store.commit('SHOW_LOGIN_DIALOG')
        }
    }
    return Promise.reject({ status, message }) // eslint-disable-line
}, errorHandler)

Vue.prototype.$ajax = request

export default request

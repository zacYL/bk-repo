/*
 * @Date: 2024-11-01 22:08:22
 * @LastEditors: xiaoshan
 * @LastEditTime: 2025-02-21 14:48:02
 * @FilePath: /artifact/src/frontend/devops-repository/src/utils/request.js
 */
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
    const { data: { data, message, error, errors }, status } = response

    // 用于处理仓库列表helm仓库返回数据格式无法统一，造成的基础信息显示错误问题
    if (status === 404 && error) {
        return {
            basic: {},
            metadata: [],
            _error: {
                error,
                status,
                message
            }
        }
    }

    // 正常逻辑
    if (status === 200 || status === 206 || status === 201) {
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
    // 当用户没有权限去删除制品时，后端返回的报错信息(因为客户端上传需要这种格式的报错信息)是使用的{error:'xxxx'}
    // 此时前端就需要先返回message的报错信息，如果message不存在则使用error
    // 当后台报错时需要将后台返回的错误状态码及相应信息返回，用于求他地方做自定义报错及处理逻辑
    return Promise.reject({ status, message:errors ? (errors?.[0]?.message|| '未知错误') :  (message || error || '未知错误'), error: response.data }) // eslint-disable-line
}, errorHandler)

Vue.prototype.$ajax = request

export default request

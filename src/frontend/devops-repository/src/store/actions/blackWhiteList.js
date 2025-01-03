/*
 * @Date: 2024-11-29 21:39:00
 * @LastEditors: xiaoshan
 * @LastEditTime: 2025-01-03 17:00:03
 * @FilePath: /artifact/src/frontend/devops-repository/src/store/actions/blackWhiteList.js
 */
import Vue from 'vue'

const Prefix = 'repository/api'

export default {
    /**
     * @description: 新增白名单/黑名单
     * @param {*} _
     * @param {*} body
     * @return {*}
     */
    createBlackWhiteList (_, { body }) {
        return Vue.prototype.$ajax.post(
            `${Prefix}/package/access-rule/create`,
            body
        )
    },

    /**
     * @description: 校验是否在黑白名单里面
     * @param {*} _
     * @param {*} projectId
     * @param {*} repoName
     * @param {*} body
     * @return {*}
     */
    blackWhiteListCheck (_, { projectId, repoName, body }) {
        return Vue.prototype.$ajax.get(
            `${Prefix}/package/access-rule/check/ ${projectId}/${repoName}/`,
            {
                params: body
            }
        )
    },
    /**
     * @description: 删除白名单/黑名单
     * @param {*} _
     * @param {*} body
     * @return {*}
     */
    deleteBlackWhiteList (_, { body }) {
        return Vue.prototype.$ajax.delete(
            `${Prefix}/package/access-rule/delete`,
            {
                params: body
            }
        )
    },
    /**
     * @description: 获取白名单/黑名单列表
     * @param {*} _
     * @param {*} body
     * @return {*}
     */
    getBlackWhiteRecords (_, body) {
        return Vue.prototype.$ajax.get(
            `${Prefix}/package/access-rule/page`,
            {
                params: body
            }
        )
    }
}

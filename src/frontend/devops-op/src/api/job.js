/*
 * @Date: 2024-11-01 22:08:22
 * @LastEditors: xiaoshan
 * @LastEditTime: 2024-11-29 17:41:28
 * @FilePath: /artifact/src/frontend/devops-op/src/api/job.js
 */
import request from '@/utils/request'

const PREFIX_SERVICES = '/job/api/job'

export function jobs () {
    return request({
        url: `${PREFIX_SERVICES}/detail`,
        method: 'get'
    })
}

export function update (name, enabled, running) {
    return request({
        url: `${PREFIX_SERVICES}/update/${name}`,
        method: 'put',
        params: {
            enabled: enabled,
            running: running
        }
    })
}

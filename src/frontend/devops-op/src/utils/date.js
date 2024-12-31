/*
 * @Date: 2024-11-01 22:08:22
 * @LastEditors: xiaoshan
 * @LastEditTime: 2024-12-30 09:51:04
 * @FilePath: /artifact/src/frontend/devops-op/src/utils/date.js
 */
import moment from 'moment'

const normalDateType = 'YYYY-MM-DD HH:mm:ss'

export function formatNormalDate (date) {
    return date != null ? moment(date).format(normalDateType) : null
}

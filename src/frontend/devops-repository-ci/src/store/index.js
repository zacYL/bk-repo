import { createExtStore } from '@repository/store'
import extStoreObject from './extStore'

//  导出 createExtStore供软件源使用
export { createExtStore }

export default createExtStore(extStoreObject)

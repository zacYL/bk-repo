import Vue from 'vue'
import App from './App'
import createRouter from './router'
import store from './store'
import '@/utils/request'

import Icon from '@/components/Icon'
import createLocale from '../../locale'
import '@icon-cool/bk-icon-devops/src/index'
import { throttleMessage } from './utils'

import bkMagic from 'bk-magic-vue'
// 全量引入 bk-magic-vue 样式
require('bk-magic-vue/dist/bk-magic-vue.min.css')

const requireAll = requireContext => requireContext.keys().map(requireContext)
const req = require.context('@/images', false, /\.svg$/)
requireAll(req)

const { i18n, setLocale } = createLocale(require.context('@locale/source/', false, /\.json$/))

Vue.component('Icon', Icon)

Vue.use(bkMagic)

Vue.prototype.$setLocale = setLocale
Vue.prototype.$bkMessage = throttleMessage(Vue.prototype.$bkMessage, 3500)

window.sourceVue = new Vue({
    router: createRouter(store),
    el: '#app',
    i18n,
    store,
    components: {
        App
    },
    template: '<App/>'
})

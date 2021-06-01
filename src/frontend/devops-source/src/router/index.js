import Vue from 'vue'
import Router from 'vue-router'
import routerArr from './router'

Vue.use(Router)

const createRouter = (store) => {
    const router = new Router({
        mode: 'history',
        routes: routerArr
    })

    router.beforeEach((to, from, next) => {
        if (to.params.repoName) {
            Vue.prototype.$ajax.get(
                `repository/api/repo/exist/${PROJECT_ID}/${to.params.repoName}`
            ).then(res => {
                if (res) next()
                else {
                    Vue.prototype.$bkMessage({
                        theme: 'error',
                        message: '仓库权限不足或仓库不存在'
                    })
                    next({ name: 'searchRepoList' })
                }
            }).catch(() => {
                next({ name: 'searchRepoList' })
            })
        } else next()
    })

    return router
}

export default createRouter

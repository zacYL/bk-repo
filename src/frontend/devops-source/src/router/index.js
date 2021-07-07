import Vue from 'vue'
import Router from 'vue-router'
import routerArr from './router'

Vue.use(Router)

function storageFrequencyRepo ({ repoType: type, repoName: name }) {
    const frequencyRepoList = localStorage.getItem('_frequencyRepo') ? JSON.parse(localStorage.getItem('_frequencyRepo')) : []
    const existIndex = frequencyRepoList.findIndex(repo => repo.name === name)
    if (~existIndex) {
        frequencyRepoList.splice(existIndex, 1)
    }
    frequencyRepoList.unshift({ type, name })
    localStorage.setItem('_frequencyRepo', JSON.stringify(frequencyRepoList.slice(0, 10)))
}

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
                if (res) {
                    storageFrequencyRepo(to.params)
                    next()
                } else {
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

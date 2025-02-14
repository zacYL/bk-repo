import Vue from 'vue'
import { mapState, mapMutations, mapActions } from 'vuex'
import ConfirmDialog from '@repository/components/ConfirmDialog'
import GlobalUploadViewport from '@repository/components/GlobalUploadViewport'
export default {
    name: 'App',
    components: { ConfirmDialog, GlobalUploadViewport },
    computed: {
        ...mapState(['userInfo', 'projectList']),
        projectId () {
            return this.$route.params.projectId
        },
        // 获取localStorage中存储的当前项目ID
        currentStorageProjectId () {
            return this.projectId || localStorage.getItem('projectId') || undefined
        }
    },
    watch: {
        '$route.fullPath' (val) { // 同步地址到蓝鲸Devops
            this.$syncUrl?.(val.replace(/^\/[a-zA-Z0-9]+\//, '/'))
        },
        // 当选择的项目ID改变时需要重新调用后端接口获取当前用户在当前项目的操作权限
        currentStorageProjectId: {
            handler (value) {
                value && this.getPermission(value)
            },
            immediate: true
        }
    },
    created () {
        const callback = e => {
            const instance = (e.reason || e)
            if (instance instanceof Error) {
                console.error(e)
            } else {
                if (instance.content) {
                    // bk-form表单校验
                } else {
                    instance.message && this.$bkMessage({
                        message: instance.message,
                        theme: 'error'
                    })
                }
            }
        }
        window.addEventListener('unhandledrejection', callback)
        Vue.config.errorHandler = callback
    },
    methods: {
        ...mapMutations(['SET_USER_INFO', 'SET_USER_LIST', 'SET_PROJECT_LIST']),
        ...mapActions([
            'getOperationPermission'
        ]),
        goHome (projectId) {
            const params = projectId ? { projectId } : {}
            this.$router.replace({
                name: 'repoList',
                params
            })
        },
        // 切换或者选择项目之后，需要加载后端权限的接口，用户控制相关操作权限
        getPermission (projectId) {
            this.getOperationPermission({ projectId })
        },
        loadDevopsUtils (src) {
            window.Vue = Vue
            const script = document.createElement('script')
            script.type = 'text/javascript'
            script.src = src
            document.getElementsByTagName('head')[0].appendChild(script)
            script.onload = () => {
                this.$syncUrl?.(this.$route.fullPath.replace(/^\/[a-zA-Z0-9]+\//, '/'))
                this.$changeActiveRoutes?.(this.$route?.meta?.breadcrumb?.map(v => v.name) || [])
                window.globalVue.$on('change::$currentProjectId', data => { // 蓝鲸Devops选择项目时切换
                    localStorage.setItem('projectId', data.currentProjectId)
                    this.getPermission(data.currentProjectId)
                    if (this.projectId !== data.currentProjectId) {
                        this.goHome(data.currentProjectId)
                    }
                })

                window.globalVue.$on('change::$routePath', data => { // 蓝鲸Devops切换路径
                    // 制品库内部此时的这个iframe_url为以/ui/结尾的，如修改则需要修改此处判断
                    if (/\/ui\/$/.test(data.routePath.iframe_url) || data.routePath.isFromGuide) {
                        this.$router.push({ name: data.routePath.routeEnglishName, path: data.routePath.path.replace(/^\/[a-zA-Z]+/, '/ui') })
                    }
                })

                window.globalVue.$on('order::backHome', data => { // 蓝鲸Devops选择项目时切换
                    this.goHome()
                })

                window.globalVue.$on('change::$projectList', data => { // 获取项目列表
                    this.SET_PROJECT_LIST(data.projectList)
                })

                window.globalVue.$on('order::syncLocale', locale => {
                    this.$setLocale?.(locale)
                })

                window.globalVue.$on('change::$userInfo', data => { // 用户信息
                    this.SET_USER_INFO(data.userInfo)
                })

                window.globalVue.$on('change::$userList', data => { // 用户信息
                    this.SET_USER_LIST(data.userList)
                })
            }
        }
    }
}

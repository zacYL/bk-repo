<template>
    <div class="bkrepo-main flex-column" v-bkloading="{ isLoading }">
        <breadcrumb class="repo-breadcrumb">
            <bk-breadcrumb-item :to="{ name: 'repoList' }">
                <svg width="48" height="17" style="vertical-align:-3px">
                    <use xlink:href="#vpack" />
                </svg>
            </bk-breadcrumb-item>
        </breadcrumb>
        <router-view class="m10 bkrepo-main-container"></router-view>
        <ConfirmDialog />
    </div>
</template>

<script>
    import Breadcrumb from '@/components/Breadcrumb/topBreadcrumb'
    import ConfirmDialog from '@/components/ConfirmDialog'
    import Vue from 'vue'
    import { mapState, mapMutations, mapActions } from 'vuex'
    export default {
        name: 'App',
        components: { Breadcrumb, ConfirmDialog },
        data () {
            return {
                isLoading: false
            }
        },
        computed: {
            ...mapState(['projectList']),
            projectId () {
                return this.$route.params.projectId
            }
        },
        watch: {
            '$route.fullPath' (val) { // 同步地址到蓝鲸Devops
                this.$syncUrl && this.$syncUrl(val.replace(/^\/ui\//, '/'))
            }
        },
        async created () {
            const urlProjectId = (location.pathname.match(/\/ui\/([^/]+)/) || [])[1]
            const localProjectId = localStorage.getItem('projectId')
            // 加载hook
            window.Vue = Vue
            const script = document.createElement('script')
            script.type = 'text/javascript'
            script.src = DEVOPS_SITE_URL + '/console/static/devops-utils.js'
            document.getElementsByTagName('head')[0].appendChild(script)
            script.onload = () => {
                this.$syncUrl(this.$route.fullPath.replace(/^\/ui\//, '/'))
                window.globalVue.$on('change::$currentProjectId', data => { // 蓝鲸Devops选择项目时切换
                    localStorage.setItem('projectId', data.currentProjectId)
                    if (this.projectId !== data.currentProjectId) {
                        this.goHome(data.currentProjectId)
                    }
                })
                window.globalVue.$on('change::$routePath', data => { // 蓝鲸Devops切换路径
                    this.$router.push({ name: data.routePath.englishName })
                })
                window.globalVue.$on('order::backHome', data => { // 蓝鲸Devops选择项目时切换
                    this.goHome()
                })

                window.globalVue.$on('change::$projectList', data => { // 获取项目列表
                    this.SET_PROJECT_LIST(data.projectList)
                })

                window.globalVue.$on('order::syncLocale', locale => {
                    this.$setLocale(locale)
                })

                window.globalVue.$on('change::$userInfo', data => { // 用户信息
                    this.SET_USER_INFO(data.userInfo)
                })

                window.globalVue.$on('change::$userList', data => { // 用户信息
                    this.SET_USER_LIST(data.userList)
                })
            }
            localStorage.setItem('projectId', urlProjectId || localProjectId || '')
            !urlProjectId && this.$router.replace({
                name: 'repoList',
                params: {
                    projectId: urlProjectId || localProjectId || ''
                }
            })
            const callback = e => {
                this.$bkMessage({
                    message: (e.reason || e).message,
                    theme: 'error'
                })
            }
            window.addEventListener('unhandledrejection', callback)
            Vue.config.errorHandler = callback
        },
        methods: {
            ...mapMutations(['SET_USER_INFO', 'SET_USER_LIST', 'SET_PROJECT_LIST']),
            ...mapActions(['getProjectList', 'ajaxUserInfo']),
            goHome (projectId) {
                const params = projectId ? { projectId } : {}
                this.$router.replace({
                    name: 'repoList',
                    params
                })
            }
        }
    }
</script>
<style lang="scss">
@import '@root/scss/index';
.bkrepo-main {
    height: 100%;
    background-color: var(--bgColor);
    .repo-breadcrumb {
        height: 20px;
        margin: 10px 10px 0;
    }
    .bkrepo-main-container {
        flex: 1;
        overflow: hidden;
    }
}
</style>

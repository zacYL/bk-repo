<template>
    <div class="source-main-container" v-bkloading="{ isLoading }">
        <router-view></router-view>
    </div>
</template>

<script>
    import Vue from 'vue'
    import { mapMutations } from 'vuex'
    export default {
        name: 'App',
        data () {
            return {
                isLoading: false
            }
        },
        watch: {
            '$route.fullPath' (val) { // 同步地址到蓝鲸Devops
                this.$syncUrl && this.$syncUrl(val.replace(/^\/bksoftware/, ''))
            }
        },
        async created () {
            window.Vue = Vue

            const script = document.createElement('script')
            script.type = 'text/javascript'
            script.src = DEVOPS_SITE_URL + '/console/static/devops-utils.js'
            document.getElementsByTagName('head')[0].appendChild(script)
            script.onload = () => {
                this.$syncUrl(this.$route.fullPath.replace(/^\/bksoftware/, ''))

                window.globalVue.$on('order::backHome', () => { // 蓝鲸Devops选择项目时切换
                    this.goHome()
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
            ...mapMutations(['SET_USER_INFO', 'SET_USER_LIST']),
            goHome () {
            }
        }
    }
</script>
<style lang="scss">
@import '@/scss/index';
.source-main-container {
    height: 100%;
    padding: 20px;
    > * {
        height: 100%;
    }
}
</style>

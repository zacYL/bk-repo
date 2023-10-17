<template>
    <div class="bkrepo-main flex-column">
        <Header v-if="!ciMode" />
        <router-view class="bkrepo-main-container"></router-view>
        <ConfirmDialog />
        <GlobalUploadViewport />
        <Login v-if="!ciMode" />
    </div>
</template>

<script>
    import Header from '@/components/Header'
    import Login from '@repository/components/Login'
    import { mapActions } from 'vuex'
    import cookies from 'js-cookie'
    import mixin from '@repository/AppMixin'
    export default {
        components: { Header, Login },
        mixins: [mixin],
        data () {
            return {
                ciMode: MODE_CONFIG === 'ci'
            }
        },
        created () {
            // 浏览器标签头展示文案
            document.title = this.$t('softwareTitle')
            const username = cookies.get('bk_uid')
            username && this.SET_USER_INFO({ username })

            if (this.ciMode) {
                const url = /^https?/.test(DEVOPS_SITE_URL)
                    ? DEVOPS_SITE_URL + '/console/static/devops-utils.js'
                    : '/ui/devops-utils.js'
                this.loadDevopsUtils(url)
            } else {
                this.getUserInfo()
                this.getRepoUserList()
                this.getProjectList()
            }
        },
        methods: {
            ...mapActions(['getProjectList', 'getUserInfo', 'getRepoUserList'])
        }
    }
</script>
<style lang="scss">
@import '@repository/scss/index';
.bkrepo-main {
    height: 100%;
    background-color: var(--bgWeightColor);
    .bkrepo-main-container {
        flex: 1;
        overflow: hidden;
    }
}
</style>

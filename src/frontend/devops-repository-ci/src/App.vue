<template>
    <div class="bkrepo-main flex-column">
        <router-view class="bkrepo-main-container"></router-view>
        <ConfirmDialog />
    </div>
</template>

<script>
    import mixin from '@repository/AppMixin'
    export default {
        mixins: [mixin],
        created () {
            const urlProjectId = (location.pathname.match(/\/ui\/([^/]+)/) || [])[1]
            const localProjectId = localStorage.getItem('projectId')

            this.loadDevopsUtils(DEVOPS_SITE_URL + '/console/static/devops-utils.js')

            localStorage.setItem('projectId', urlProjectId || localProjectId || '')
            !urlProjectId && this.$router.replace({
                name: 'repoList',
                params: {
                    projectId: urlProjectId || localProjectId || ''
                }
            })
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

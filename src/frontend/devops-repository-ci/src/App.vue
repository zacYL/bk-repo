<!--
 * @Date: 2024-11-01 22:08:22
 * @LastEditors: xiaoshan
 * @LastEditTime: 2025-01-06 17:59:16
 * @FilePath: /artifact/src/frontend/devops-repository-ci/src/App.vue
-->
<template>
    <div class="bkrepo-main flex-column">
        <router-view class="bkrepo-main-container"></router-view>
        <GlobalUploadViewport />
        <ConfirmDialog />
    </div>
</template>

<script>
    import mixin from '@repository/AppMixin'
    import { mapActions } from 'vuex'
    export default {
        mixins: [mixin],
        created () {
            this.loadDevopsUtils(DEVOPS_SITE_URL + '/console/static/devops-utils.js')
            this.getAdminInfo()
        },
        methods: {
            ...mapActions([
                'getAdminInfo'
            ])
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

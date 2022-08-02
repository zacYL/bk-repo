<template>
    <div class="bkrepo-main flex-column">
        <router-view class="bkrepo-main-container"></router-view>
        <GlobalUploadViewport />
        <ConfirmDialog />
    </div>
</template>

<script>
    import mixin from '@repository/AppMixin'
    export default {
        mixins: [mixin],
        created () {
            this.loadDevopsUtils(DEVOPS_SITE_URL + '/console/static/devops-utils.js')
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

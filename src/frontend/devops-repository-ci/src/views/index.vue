<template>
    <div class="m10 bkrepo-view-main flex-column flex-1">
        <breadcrumb class="mb10 repo-breadcrumb">
            <bk-breadcrumb-item :to="{ name: 'repoList' }">
                <svg width="48" height="16" style="vertical-align:-3px">
                    <use xlink:href="#vpack" />
                </svg>
            </bk-breadcrumb-item>
        </breadcrumb>
        <router-view class="flex-1"></router-view>
    </div>
</template>
<script>
    import Breadcrumb from '@repository/components/Breadcrumb/topBreadcrumb'
    export default {
        components: { Breadcrumb }
    }
</script>
<style lang="scss" scoped>
.bkrepo-view-main {
    height: calc(100% - 20px);
    .repo-breadcrumb {
        height: 20px;
    }
}
</style>

<template>
    <div class="m10 bkrepo-view-main flex-column flex-1">
        <breadcrumb class="mb10 repo-breadcrumb">
            <bk-breadcrumb-item :to="{ name: breadcrumbName }">
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
    import { mapActions } from 'vuex'
    export default {
        components: { Breadcrumb },
        computed: {
            breadcrumb () {
                return this.$route.meta.breadcrumb || []
            },
            // 当前面包屑跳转的路由名，当面包屑没有值时跳转到项目首页，当面包屑有值时点击CPack图标跳转到二级菜单的首页
            breadcrumbName () {
                return (this.breadcrumb?.length || 0) > 0 ? this.breadcrumb[0]?.name || 'repoList' : 'repoList'
            }
        },
        created () {
            this.getClusterList()
            this.getModuleInfo()
        },
        methods: {
            ...mapActions(['getClusterList', 'getModuleInfo'])
        }
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

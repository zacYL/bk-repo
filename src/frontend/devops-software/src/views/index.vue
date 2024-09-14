<template>
    <div class="bkrepo-view flex-align-center">
        <div class="repo-header pl30 pr20">
            <div
                v-for="name in menuList.project"
                :key="name"
                style="margin-right: 30px;"
                @click="menuTo(name)">
                <a
                    class="nav-submain-item f14 flex-align-center"
                    :title="$t(name)"
                    :class="{ 'active-route': $route.meta.breadcrumb.find(route => route.name === name) }"
                    onclick="return false"
                    :href="''">
                    <span class="text-overflow">
                        {{$t(name)}}
                    </span>
                </a>
            </div>
        </div>
        <breadcrumb v-if="breadcrumb.length > 1" class="pl10 repo-breadcrumb">
        </breadcrumb>
        <div class="m6 bkrepo-view-main flex-column flex-1">
            <router-view class="flex-1"></router-view>
        </div>
    </div>
</template>
<script>
    import Breadcrumb from '@repository/components/Breadcrumb/topBreadcrumb'
    export default {
        components: { Breadcrumb },
        data () {
            return {}
        },
        computed: {
            menuList () {
                return {
                    project: [
                        'repoList',
                        'repoSearch'
                    ]
                }
            },
            breadcrumb () {
                return this.$route.meta.breadcrumb || []
            },
            // 当前面包屑跳转的路由名，当面包屑没有值时跳转到项目首页，当面包屑有值时点击CPack图标跳转到二级菜单的首页
            breadcrumbName () {
                return (this.breadcrumb?.length || 0) > 0 ? this.breadcrumb[0]?.name || 'repoList' : 'repoList'
            }
        },
        methods: {
            menuTo (name) {
                this.$router.push({ name })
            },
            createRoute (name) {
                
            }
        }
    }
</script>
<style lang="scss" scoped>
.bkrepo-view {
    height: 100%;
    flex-direction: column;
    .repo-header {
        min-height: 60px;
        background-color: #fff;
        display: flex;
        align-items: center;
        width: 100%;
        &::after {
            content: '';
            height: 1px;
            width: 100%;
            position: absolute;
            top: 59px;
            left: 0;
            z-index: 1;
            background-color: #E5EAF0;
        }
        .nav-submain-item {
            height: 60px;
            color: #081E40;
            &:hover {
                color: #016BFF;
            }
            &.active-route {
                font-weight: 600;
                color: #016BFF;
                position: relative;
                &::before {
                    content: "";
                    width: 100%;
                    height: 2px;
                    background: #016BFF;
                    position: absolute;
                    top: 58px;
                    z-index: 2;
                }
            }
        }
    }
    .repo-breadcrumb {
        height: 42px;
        width: 100%;
        background: #fff;
    }
    .bkrepo-view-main {
        height: calc(100% - 20px); // margin
        width: calc(100% - 12px);
    }
}
</style>

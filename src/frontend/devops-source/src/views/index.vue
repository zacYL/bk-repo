<template>
    <div class="flex-align-center">
        <ul v-if="userInfo.admin" class="mr20 pt5 devops-source-nav">
            <li v-for="item in nav" :key="item.name">
                <router-link
                    class="nav-item flex-align-center"
                    :to="{ name: item.name }">
                    <Icon class="mr10" :name="item.icon" size="16" />
                    <span class="text-overflow">{{ item.label }}</span>
                </router-link>
            </li>
        </ul>
        <router-view class="devops-source-main"></router-view>
    </div>
</template>
<script>
    import { mapState, mapActions } from 'vuex'
    export default {
        name: 'container',
        data () {
            return {
                nav: [
                    // {
                    //     name: 'overview',
                    //     label: '概览',
                    //     icon: 'source-overview'
                    // },
                    {
                        name: 'manage',
                        label: '仓库管理',
                        icon: 'source-manage'
                    },
                    {
                        name: 'search',
                        label: '搜索制品',
                        icon: 'source-search'
                    },
                    {
                        name: 'audit',
                        label: '审计日志',
                        icon: 'source-audit'
                    },
                    {
                        name: 'admin',
                        label: '管理员',
                        icon: 'source-admin'
                    }
                ]
            }
        },
        computed: {
            ...mapState(['userInfo'])
        },
        created () {
            this.getRepoListAll()
        },
        methods: {
            ...mapActions(['getRepoListAll'])
        }
    }
</script>
<style lang="scss" scoped>
@import '@/scss/conf';
.devops-source-nav {
    height: 100%;
    flex-basis: 240px;
    background-color: white;
    > li {
        cursor: pointer;
        .nav-item {
            height: 42px;
            font-size: 14px;
            padding: 0 20px;
            border-right: 3px solid;
            border-right-color: transparent;
            &.router-link-active, &:hover {
                color: $primaryColor;
                background-color: $primaryLightColor;
            }
            &.router-link-active {
                border-right-color: $primaryColor;
            }
        }
    }
}
.devops-source-main {
    height: 100%;
    flex: 1;
    overflow: hidden;
    background-color: white;
}
</style>

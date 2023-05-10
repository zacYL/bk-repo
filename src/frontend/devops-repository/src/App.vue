<template>
    <div class="bkrepo-main flex-column">
        <Header v-if="!ciMode" />
        <template>
            <div v-if="!ciMode && !projectList.length && !userInfo.admin" v-bkloading="{ isLoading }" class="empty-project-container">
                <img src="/ui/no-data.png" width="400" />
                <span class="empty-project-title">
                    您没有参与任何项目，请联系管理员为您关联项目
                </span>
            </div>
            <router-view v-else class="bkrepo-main-container"></router-view>
        </template>
        <ConfirmDialog />
        <GlobalUploadViewport />
        <Login v-if="!ciMode" />
    </div>
</template>

<script>
    import Header from '@repository/components/Header'
    import Login from '@repository/components/Login'
    import { mapActions } from 'vuex'
    import cookies from 'js-cookie'
    import mixin from '@repository/AppMixin'
    export default {
        components: { Header, Login },
        mixins: [mixin],
        data () {
            return {
                ciMode: MODE_CONFIG === 'ci',
                isLoading: true
            }
        },
        created () {
            const username = cookies.get('bk_uid')
            username && this.SET_USER_INFO({ username })

            if (this.ciMode) {
                this.loadDevopsUtils('/ui/devops-utils.js')
                // 请求管理员信息
                this.getUserInfo().then((userInfo) => {
                    userInfo.admin && this.getClusterList()
                })
            } else {
                const urlProjectId = (location.pathname.match(/^\/[a-zA-Z0-9]+\/([^/]+)/) || [])[1]
                const localProjectId = localStorage.getItem('projectId')
                Promise.all([this.getUserInfo(), this.getProjectList(), this.getRepoUserList()]).then(([userInfo]) => {
                    if (!this.ciMode && !this.projectList.length) {
                        if (userInfo.admin) {
                            // TODO: 管理员创建项目引导页
                            this.$bkMessage({
                                message: '无项目数据',
                                theme: 'error'
                            })
                            this.$router.replace({
                                name: 'projectManage',
                                params: {
                                    projectId: urlProjectId || localProjectId || 'default'
                                }
                            })
                        } else {
                            // TODO: 普通用户无项目提示页
                            this.$bkMessage({
                                message: '您没有参与任何项目，请联系管理员为您关联项目',
                                theme: 'warning'
                            })
                        }
                    } else {
                        let projectId = ''
                        if (this.projectList.find(v => v.id === urlProjectId)) {
                            projectId = urlProjectId
                        } else if (this.projectList.find(v => v.id === localProjectId)) {
                            projectId = localProjectId
                        } else {
                            projectId = (this.projectList[0] || {}).id
                        }
                        localStorage.setItem('projectId', projectId)

                        projectId && projectId !== urlProjectId && this.$router.replace({
                            name: 'repoList',
                            params: {
                                projectId
                            }
                        })
                    }
                    
                    userInfo.admin && this.getClusterList()
                }).finally(() => {
                    this.isLoading = false
                })
            }
        },
        methods: {
            ...mapActions([
                'getProjectList',
                'getUserInfo',
                'getRepoUserList',
                'getClusterList'
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
    .empty-project-container{
        display: flex;
        flex-direction:column;
        align-items: center;
        justify-content: center;
        height: calc(100% - 50px);
    }
    .empty-project-title{
        font-weight: bold;
        font-size: 16px;
    }
}
</style>

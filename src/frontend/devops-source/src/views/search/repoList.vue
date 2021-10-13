<template>
    <div class="repo-list-container">
        <header class="repo-list-header flex-align-center">
            <bk-select
                class="w140"
                v-model="query.type"
                placeholder="仓库类型">
                <bk-option
                    v-for="type in repoEnum"
                    :key="type"
                    :id="type"
                    :name="type">
                    <div class="repo-name flex-align-center">
                        <icon size="20" :name="type" />
                        <span class="ml10">{{type}}</span>
                    </div>
                </bk-option>
            </bk-select>
            <bk-input
                class="ml10 w220"
                v-model.trim="query.name"
                placeholder="仓库名称"
                :clearable="true">
            </bk-input>
            <bk-button class="ml10 pl5 pr5" theme="primary" @click="handlerPaginationChange()" icon="search">
                {{ $t('search') }}
            </bk-button>
            <div class="flex-1 flex-end-center">
                <bk-button class="pl5 pr5" theme="default" @click="$router.push({ name: 'searchOverview' })">
                    {{ $t('returnBack') }}
                </bk-button>
            </div>
        </header>
        <main class="repo-list-table" v-bkloading="{ isLoading }">
            <bk-table
                :data="repoList"
                height="calc(100% - 52px)"
                :outer-border="false"
                :row-border="false"
                size="small">
                <bk-table-column :label="$t('repoName')">
                    <template #default="{ row }">
                        <div class="repo-name flex-align-center hover-btn" @click="toRepoDetail(row)">
                            <icon size="20" :name="row.repoType" />
                            <span class="ml10">{{row.name}}</span>
                        </div>
                    </template>
                </bk-table-column>
                <bk-table-column label="仓库权限" width="250">
                    <template #default="{ row }">
                        {{ { 'ARTIFACT_READWRITE': '可推送', 'ARTIFACT_READ': '可下载' }[row.permission] }}
                    </template>
                </bk-table-column>
                <bk-table-column label="制品/文件数" prop="artifacts" width="150"></bk-table-column>
            </bk-table>
            <bk-pagination
                class="mt10"
                size="small"
                align="right"
                show-total-count
                @change="current => handlerPaginationChange({ current })"
                @limit-change="limit => handlerPaginationChange({ limit })"
                :current.sync="pagination.current"
                :limit="pagination.limit"
                :count="pagination.count"
                :limit-list="pagination.limitList">
            </bk-pagination>
        </main>
    </div>
</template>
<script>
    import { mapState, mapActions } from 'vuex'
    import { repoEnum } from '@/store/publicEnum'
    export default {
        name: 'repoList',
        data () {
            return {
                repoEnum,
                isLoading: false,
                repoList: [],
                query: {
                    usedInfo: true,
                    name: '',
                    type: ''
                },
                pagination: {
                    count: 0,
                    current: 1,
                    limit: 20,
                    limitList: [10, 20, 40]
                }
            }
        },
        computed: {
            ...mapState(['userList'])
        },
        created () {
            this.handlerPaginationChange()
        },
        methods: {
            ...mapActions([
                'getRepoList'
            ]),
            async getListData () {
                this.isLoading = true
                const { records, totalRecords } = await this.getRepoList({
                    ...this.pagination,
                    ...this.query
                }).finally(() => {
                    this.isLoading = false
                })
                this.repoList = records.map(v => ({ ...v, repoType: v.type.toLowerCase() }))
                this.pagination.count = totalRecords
            },
            handlerPaginationChange ({ current = 1, limit = this.pagination.limit } = {}) {
                this.pagination.current = current
                this.pagination.limit = limit
                this.getListData()
            },
            toRepoDetail ({ repoType, name }) {
                this.$router.push({
                    name: 'searchPackageList',
                    params: {
                        repoType,
                        repoName: name
                    }
                })
            }
        }
    }
</script>
<style lang="scss" scoped>
@import '@/scss/conf';
.repo-list-container {
    height: 100%;
    padding: 0 20px;
    background-color: white;
    .repo-list-header {
        height: 50px;
        margin-bottom: 10px;
    }
    .repo-list-table {
        height: calc(100% - 60px);
    }
}
.devops-icon {
    font-size: 16px;
}
.repo-name {
    height: 44px;
    font-size: 14px;
}
</style>

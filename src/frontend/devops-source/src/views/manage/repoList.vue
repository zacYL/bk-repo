<template>
    <div class="repo-list-container">
        <header class="mt10 mb10 flex-align-center">
            <bk-button class="pl5 pr5" :theme="'primary'" @click="toCreateRepo" icon="plus">
                {{ $t('create') }}
            </bk-button>
            <bk-select
                class="ml10 w140"
                v-model="query.type"
                :placeholder="$t('repoType')">
                <bk-option
                    v-for="type in repoEnum"
                    :key="type"
                    :id="type"
                    :name="type">
                    <div class="repo-name flex-align-center">
                        <icon size="24" :name="type" />
                        <span class="ml10">{{type}}</span>
                    </div>
                </bk-option>
            </bk-select>
            <bk-input
                class="ml10 w220"
                v-model.trim="query.name"
                :clearable="true"
                :placeholder="$t('repoName')">
            </bk-input>
            <bk-button class="ml10 pl5 pr5" theme="primary" @click="handlerPaginationChange()" icon="search">
                {{ $t('search') }}
            </bk-button>
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
                        <div class="flex-align-center" @click="toRepoDetail(row)">
                            <Icon size="24" :name="row.repoType" />
                            <span class="ml10 w220 text-overflow repo-name" :title="row.name">{{row.name}}</span>
                        </div>
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('createdDate')" width="250">
                    <template #default="{ row }">
                        {{ formatDate(row.createdDate) }}
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('createdBy')" width="250">
                    <template #default="{ row }">
                        {{ userList[row.createdBy] ? userList[row.createdBy].name : row.createdBy }}
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('operation')" width="100">
                    <template #default="{ row }">
                        <i class="hover-btn devops-icon icon-cog mr10" @click="showRepoConfig(row)"></i>
                        <i class="hover-btn devops-icon icon-delete" @click="deleteRepo(row)"></i>
                    </template>
                </bk-table-column>
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
    import { formatDate } from '@/utils'
    export default {
        name: 'repoList',
        data () {
            return {
                repoEnum,
                isLoading: false,
                repoList: [],
                query: {
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
            formatDate,
            ...mapActions(['getRepoList', 'deleteRepoList']),
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
            toCreateRepo () {
                this.$router.push({
                    name: 'createRepo'
                })
            },
            toRepoDetail ({ repoType, name }) {
                this.$router.push({
                    name: 'searchPackageList',
                    params: {
                        repoType,
                        repoName: name
                    }
                })
            },
            showRepoConfig ({ repoType, name }) {
                this.$router.push({
                    name: 'repoConfig',
                    params: {
                        repoType,
                        repoName: name
                    }
                })
            },
            deleteRepo ({ name }) {
                this.$bkInfo({
                    type: 'error',
                    title: this.$t('deleteRepoTitle'),
                    subTitle: this.$t('deleteRepoSubTitle'),
                    showFooter: true,
                    confirmFn: () => {
                        this.deleteRepoList({
                            name
                        }).then(() => {
                            this.getListData()
                            this.$bkMessage({
                                theme: 'success',
                                message: this.$t('delete') + this.$t('success')
                            })
                        })
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
    .repo-list-table {
        height: calc(100% - 52px);
    }
}
.devops-icon {
    font-size: 16px;
}
.repo-name {
    cursor: pointer;
    font-size: 14px;
    &:hover {
        color: $primaryColor;
    }
}
</style>

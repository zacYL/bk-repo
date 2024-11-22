<!--
 * @Date: 2024-11-21 15:38:37
 * @LastEditors: xiaoshan
 * @LastEditTime: 2024-11-22 21:59:02
 * @FilePath: /artifact/src/frontend/devops-repository-ci/src/views/repoGeneric/repoRecycleBin/index.vue
-->
<template>
    <div class="recycle-bin" v-bkloading="{ isLoading }">
        <div class="ml20 mr20 mt10 flex-align-center">
            <bk-input
                v-model.trim="name"
                :placeholder="$t('artifactPlaceholder')"
                clearable
                style="width: 250px;"
                right-icon="bk-icon icon-search"
                @enter="search"
                @clear="() => {
                    search
                }"></bk-input>
        </div>
        <bk-table
            class="mt10 scan-table"
            height="calc(100% - 100px)"
            :data="recycleBinList"
            :outer-border="false"
            :row-border="false"
            row-key="id"
            size="small">
            <template #empty>
                <empty-data :is-loading="isLoading"></empty-data>
            </template>
            <!-- 制品包名称 -->
            <bk-table-column :label="$t('composerInputLabel')" show-overflow-tooltip>
                <template #default="{ row }">
                    <span class="hover-btn">{{row.name}}</span>
                </template>
            </bk-table-column>
            <!-- 所属仓库 -->
            <bk-table-column :label="$t('repo')" prop="repoName" show-overflow-tooltip>
            </bk-table-column>
            <!-- 原路径 -->
            <bk-table-column :label="$t('originalPath')" prop="fullPath" show-overflow-tooltip>
            </bk-table-column>
            <!-- 制品大小 -->
            <bk-table-column :label="$t('artifactSize')" prop="size">
            </bk-table-column>
            <!-- 删除时间 -->
            <bk-table-column :label="$t('deleteTime')" prop="deleted">
            </bk-table-column>
            <!-- 删除者 -->
            <bk-table-column :label="$t('deleter')" prop="lastModifiedBy">
            </bk-table-column>
            <!-- 操作 -->
            <bk-table-column :label="$t('operation')">
                <template #default="{ row }">
                    <bk-button theme="primary" text class="mr10" @click="revert(row)">{{$t('revert')}}</bk-button>
                    <bk-button theme="danger" text @click="remove(row)">{{$t('permanentlyDelete')}}</bk-button>
                </template>
            </bk-table-column>
        </bk-table>
        <bk-pagination
            class="p10"
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
    </div>
</template>
<script>
    import { cloneDeep } from 'lodash'
    import { mapActions } from 'vuex'
    const paginationParams = {
        count: 0,
        current: 1,
        limit: 20,
        limitList: [10, 20, 40]
    }
    export default {
        name: 'repo-recycle-bin',
        props: {
            projectId: String
        },
        data () {
            return {
                isLoading: false,
                pagination: cloneDeep(paginationParams),
                recycleBinList: []
            }
        },
        created () {
            this.handlerPaginationChange({ current: 1, limit: 20 })
        },
        methods: {
            ...mapActions([
                'getRecycleBinList',
                'checkConflictPath'
            ]),
            // 分页
            handlerPaginationChange ({ current = 1, limit = this.pagination.limit } = {}) {
                this.pagination.current = current
                this.pagination.limit = limit
                this.getRecycleBinListData()
            },
            // 获取回收站列表
            getRecycleBinListData () {
                const body = {
                    page: {
                        pageNumber: this.pagination.current,
                        pageSize: this.pagination.limit
                    },
                    sort: {
                        properties: [
                            'deleted'
                        ],
                        direction: 'DESC'
                    },
                    select: ['deleted', 'fullPath', 'folder', 'name', 'size', 'lastModifiedBy', 'repoName', 'sha256', 'md5', 'projectId'],
                    rule: {
                        rules: [
                            {
                                field: 'projectId',
                                value: this.projectId,
                                operation: 'EQ'
                            },
                            {
                                field: 'repoName',
                                value: this.name,
                                operation: 'EQ'
                            },
                            {
                                field: 'deleted',
                                operation: 'NOT_NULL'
                            },
                            {
                                field: 'metadata._root_deleted_node',
                                value: true,
                                operation: 'EQ'
                            }
                        ],
                        relation: 'AND'
                    }
                }
                this.isLoading = true
                return this.getRecycleBinList(
                    body
                ).then(res => {
                    this.recycleBinList = res.records
                    this.pagination.count = res.totalRecords
                }).finally(() => {
                    this.isLoading = false
                })
            },
            // 搜索
            search () {
                this.$nextTick(() => {
                    this.handlerPaginationChange({ current: 1 })
                })
            },
            resetTable () {
                let current = 0
                if (this.recycleBinList.length === 1) {
                    if (this.pagination.current === 1) {
                        current = 1
                    } else {
                        current = this.pagination.current - 1
                    }
                } else {
                    current = this.pagination.current
                }
                this.handlerPaginationChange({ current: current })
            },
            revert (row) {
                // todo 缺少校验冲突接口
                this.checkConflictPath(row.fullPath).then((res) => {
                    const cb = () => {
                        // todo 缺少恢复接口
                        return Promise.resolve().then(() => {
                            this.$bkMessage({
                                theme: 'success',
                                message: this.$t('revertSuccess')
                            })
                        }).catch(() => {
                            this.$bkMessage({
                                theme: 'error',
                                message: this.$t('revertFail')
                            })
                        })
                    }
                    
                    if (res) {
                        cb().then(() => {
                            this.resetTable()
                        })
                    } else {
                        this.$bkInfoDevopsConfirm({
                            subTitle: this.$t('artifactConflictRevert'),
                            theme: 'danger',
                            okText: this.$t('revert'),
                            confirmFn: () => {
                                cb().then(() => {
                                    this.resetTable()
                                })
                            }
                        })
                    }
                }).catch(error => {
                    console.error(error)
                    this.$bkMessage({
                        theme: 'error',
                        message: this.$t('checkErr')
                    })
                })
            },
            remove (row) {
                this.$bkInfoDevopsConfirm({
                    subTitle: this.$t('permanentlyDeleteTips'),
                    theme: 'danger',
                    confirmFn: () => {
                        // todo 缺一个删除接口
                        Promise.resolve().then(() => {
                            this.$bkMessage({
                                theme: 'success',
                                message: this.$t('removeSuccess')
                            })
                            this.resetTable()
                        }).catch(() => {
                            this.$bkMessage({
                                theme: 'error',
                                message: this.$t('removeFail')
                            })
                        })
                    }
                })
            }
        }
    }
</script>
<style lang="scss" scoped>
.recycle-bin {
    height: 100%;
    width: 100%;
    background-color: white;
}
</style>

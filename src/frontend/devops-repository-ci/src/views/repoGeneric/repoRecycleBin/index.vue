<!--
 * @Date: 2024-11-21 15:38:37
 * @LastEditors: xiaoshan
 * @LastEditTime: 2024-12-04 16:28:32
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
            <bk-table-column :label="$t('name')" show-overflow-tooltip>
                <template #default="{ row }">
                    <span>{{row.name}}</span>
                </template>
            </bk-table-column>
            <!-- 原路径 -->
            <bk-table-column :label="$t('originalPath')" prop="fullPath" show-overflow-tooltip>
            </bk-table-column>
            <!-- 制品大小 -->
            <bk-table-column :label="$t('artifactSize')" prop="size">
                <template #default="{ row }">
                    <span>{{convertFileSize(row.size)}}</span>
                </template>
            </bk-table-column>
            <!-- 删除时间 -->
            <bk-table-column :label="$t('deleteTime')" prop="deleted" show-overflow-tooltip>
                <template #default="{ row }">
                    <span>{{formatDate(row.deleted)}}</span>
                </template>
            </bk-table-column>
            <!-- 删除者 -->
            <bk-table-column :label="$t('deleter')" prop="lastModifiedBy" show-overflow-tooltip>
                <template #default="{ row }">
                    <span>{{ userList[row.lastModifiedBy] ? userList[row.lastModifiedBy].name : row.lastModifiedBy }}</span>
                </template>
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
    import { mapActions, mapState } from 'vuex'

    import { convertFileSize, formatDate } from '@repository/utils'
    const paginationParams = {
        count: 0,
        current: 1,
        limit: 20,
        limitList: [10, 20, 40]
    }
    export default {
        name: 'repo-recycle-bin',
        data () {
            return {
                isLoading: false,
                pagination: cloneDeep(paginationParams),
                recycleBinList: []
            }
        },
        computed: {
            ...mapState(['userList']),
            projectId () {
                return this.$route.query.projectId
            },
            repoName () {
                return this.$route.query.repoName
            }
        },
        created () {
            this.getRecycleBinPermission()
            this.handlerPaginationChange({ current: 1, limit: 20 })
        },
        methods: {
            formatDate,
            convertFileSize,
            ...mapActions([
                'getRecycleBinPermission',
                'getRecycleBinList',
                'checkConflictPath',
                'nodeRevert',
                'nodeDelete',
                'getNodeDetail'
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
                    select: ['deleted', 'fullPath', 'folder', 'name', 'size', 'lastModifiedBy', 'repoName', 'sha256', 'md5', 'projectId', 'path'],
                    rule: {
                        rules: [
                            {
                                field: 'projectId',
                                value: this.projectId,
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
                            },
                            {
                                field: 'repoName',
                                value: this.repoName,
                                operation: 'EQ'
                            },
                            ...this.name
                                ? [{
                                    field: 'name',
                                    value: '*' + this.name + '*',
                                    operation: 'MATCH_I'
                                }]
                                : []
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
                const generateRevertPromise = (type) => {
                    return this.nodeRevert({
                        projectId: this.projectId,
                        repoName: this.repoName,
                        fullPath: row.fullPath,
                        deleteTime: new Date(row.deleted).getTime(),
                        type
                    })
                }

                const cb = (revertPromise) => {
                    return revertPromise.then(() => {
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

                const revertFn = () => {
                    // 校验冲突路径
                    this.checkConflictPath({
                        projectId: this.projectId,
                        repoName: this.repoName,
                        fullPath: row.fullPath
                    }).then((res) => {
                        this.$bkInfoDevopsConfirm({
                            subTitle: this.$t('artifactConflictRevert', [row.name]),
                            theme: 'danger',
                            okText: this.$t('overwrite'),
                            confirmFn: () => {
                                const conflictNodeRevert = generateRevertPromise('OVERWRITE')
                                // 覆盖原有文件
                                cb(conflictNodeRevert).then(() => {
                                    this.resetTable()
                                })
                            }
                        })
                    }).catch((error) => {
                        if (error.status.toString() === '400') {
                            this.$bkInfoDevopsConfirm({
                                subTitle: this.$t('artifactRevert', [row.name]),
                                theme: 'success',
                                okText: this.$t('revert'),
                                confirmFn: () => {
                                    const nodeRevert = generateRevertPromise('FAILED')
                                    // 恢复文件
                                    cb(nodeRevert).then(() => {
                                        this.resetTable()
                                    })
                                }
                            })
                        } else {
                            this.$bkMessage({
                                theme: 'error',
                                message: this.$t('checkErr')
                            })
                        }
                    })
                }
                // 如果是根目录，直接执行恢复函数
                if (row.path === '/') {
                    revertFn()
                } else {
                    this.getNodeDetail({
                        projectId: this.projectId,
                        repoName: this.repoName,
                        fullPath: row.path.slice(0, -1),
                        localNode: true
                    }).then((res) => {
                        // 处理成功的情况
                        if (res.folder) {
                            revertFn()
                        } else {
                            this.$bkMessage({
                                theme: 'error',
                                message: this.$t('filePathNoExit', [row.path.slice(0, -1)])
                            })
                        }
                    }).catch((error) => {
                        if (error.status.toString() === '400') {
                            this.$bkMessage({
                                theme: 'error',
                                message: this.$t('filePathNoExit', [row.path.slice(0, -1)])
                            })
                        }
                    })
                }
            },
            remove (row) {
                this.$bkInfoDevopsConfirm({
                    subTitle: this.$t('permanentlyDeleteTips'),
                    theme: 'danger',
                    confirmFn: () => {
                        // 彻底删除
                        this.nodeDelete({
                            projectId: this.projectId,
                            repoName: this.repoName,
                            fullPath: row.fullPath,
                            deleteTime: new Date(row.deleted).getTime()
                        }).then(() => {
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

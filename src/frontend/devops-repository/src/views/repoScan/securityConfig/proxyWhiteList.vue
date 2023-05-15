<template>
    <div class="proxy-manage-container" v-bkloading="{ isLoading }">
        <div class="mt10 flex-between-center">
            <div class="btn-group">
                <bk-button class="ml10" theme="primary" icon="plus" @click="handleClickShowDialog('add')">添加</bk-button>
                <bk-button class="ml10" @click="handleClickShowDialog('api')">API调用</bk-button>
                <bk-button class="ml10" @click="handleClickShowDialog('status')">启用状态</bk-button>
            </div>
            <!-- 筛选框 -->
            <div class="mr20 flex-align-center">
                <bk-select
                    @change="handlerPaginationChange"
                    v-model.trim="params.type"
                    placeholder="全部仓库类型"
                    style="width: 160px;">
                    <bk-option v-for="item in artifactTypeList" :key="item" :id="item" :name="item"></bk-option>
                </bk-select>
                <bk-input
                    v-model.trim="params.packageKey"
                    class="ml10"
                    style="width: 230px;"
                    placeholder="请输入制品名称, 按Enter键搜索"
                    clearable
                    @enter="handlerPaginationChange"
                    @clear="handlerPaginationChange"
                    right-icon="bk-icon icon-search"
                />
            </div>
            <!-- 筛选框 /-->
        </div>

        <!-- 表格 -->
        <bk-table
            class="mt10"
            height="calc(100% - 100px)"
            :data="proxyWhiteList"
            :outer-border="false"
            :row-border="false"
            size="small"
        >
            <template #empty>
                <empty-data :is-loading="isLoading"></empty-data>
            </template>
            <bk-table-column label="制品名称" prop="packageKey" min-width="300px" show-overflow-tooltip />
            <bk-table-column label="版本" v-slot="{ row }" show-overflow-tooltip>
                <div>{{ row.versions.join(', ') }}</div>
            </bk-table-column>
            <bk-table-column label="制品类型" prop="type" width="120px" />
            <bk-table-column label="操作" v-slot="{ row }" width="150px">
                <bk-button class="mr10" theme="primary" text @click="handleClickShowDialog('edit', row)">编辑</bk-button>
                <bk-button theme="primary" text @click="handleClickDelArtifact(row)">删除</bk-button>
            </bk-table-column>
        </bk-table>
        <!-- 表格 /-->

        <!-- 分页 -->
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
        <!-- 分页 /-->

        <!-- 新增制品信息dialog -->
        <api-tip-dialog
            :show="artifactDialogType === 'api'"
            @close="handleClickShowDialog('close')"
        />
        <!-- 编辑制品信息dialog -->
        <add-or-edit-artifact-dialog
            :show="['add', 'edit'].includes(artifactDialogType)"
            :type="artifactDialogType"
            :artifact="curArtifact"
            @close="handleClickShowDialog('close')"
            @update="fetchWhitelist"
        />
        <!-- 白名单启用状态dialog -->
        <white-list-status-dialog
            :show="artifactDialogType === 'status'"
            @close="handleClickShowDialog('close')"
        />
    </div>
</template>

<script>
    import { mapActions, mapState } from 'vuex'
    import AddOrEditArtifactDialog from './proxyWhiteListDialog/addOrEditArtifactDialog.vue'
    import ApiTipDialog from './proxyWhiteListDialog/apiTipDialog.vue'
    import whiteListStatusDialog from './proxyWhiteListDialog/whiteListStatusDialog.vue'

    export default {
        components: {
            AddOrEditArtifactDialog,
            ApiTipDialog,
            whiteListStatusDialog
        },
        data () {
            return {
                isLoading: false,
                proxyWhiteList: [],
                pagination: {
                    count: 0,
                    current: 1,
                    limit: 20,
                    limitList: [10, 20, 40]
                },
                artifactDialogType: '',
                curArtifact: {},
                params: {
                    type: '',
                    packageKey: '',
                    version: '',
                    pageNumber: 1,
                    pageSize: 20
                }
            }
        },
        computed: {
            ...mapState(['artifactTypeList']) // 二期做筛选时会用到
        },
        created () {
            if (!this.artifactTypeList.length) {
                this.initArtifactTypeList()
            }
            this.handlerPaginationChange()
        },
        methods: {
            ...mapActions([
                'getWhitelist',
                'addWhiteList',
                'delWhiteList',
                'initArtifactTypeList'
            ]),
            fetchWhitelist () {
                this.isLoading = true
                this.current = this.params.pageNumber
                return this.getWhitelist(this.params)
                    .then(res => {
                        res.records.forEach(item => {
                            item.versions = item.versions || [] // 兼容versions为null的场景
                        })
                        this.proxyWhiteList = res.records
                        this.pagination.count = res.count
                    })
                    .finally(() => {
                        this.isLoading = false
                    })
            },
            handlerPaginationChange ({ current = 1, limit = this.pagination.limit } = {}) {
                this.params.pageNumber = current
                this.params.pageSize = limit
                this.pagination.current = current
                this.pagination.limit = limit
                this.fetchWhitelist()
            },
            handleClickShowDialog (dialogType, artifact) {
                if (dialogType === 'close') {
                    this.artifactDialogType = ''
                } else {
                    this.artifactDialogType = dialogType
                    if (dialogType === 'edit') {
                        this.curArtifact = artifact
                    }
                }
            },
            handleClickDelArtifact (artifact) {
                this.$bkInfoDevopsConfirm({
                    title: '操作确认',
                    subTitle: `是否确认删除${artifact.packageKey}？`,
                    theme: 'danger',
                    confirmFn: () => {
                        this.isLoading = true
                        this.delWhiteList({ id: artifact.id })
                            .then(() => {
                                this.$bkMessage({
                                    theme: 'success',
                                    message: '删除成功'
                                })
                                this.fetchWhitelist()
                            })
                            .finally(() => {
                                this.isLoading = false
                            })
                    }
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
.proxy-manage-container {
    height: 100%;
    overflow: hidden;
    .hover-visible {
        visibility: hidden;
    }
    .hover-row .hover-visible {
        visibility: visible;
    }
}
</style>

<template>
    <div class="proxy-manage-container" v-bkloading="{ isLoading }">
        <!-- 筛选框 -->
        <div class="mt10 flex-between-center">
            <bk-button class="ml20" icon="plus" theme="primary" @click="handleClickShowDialog('add')">制品信息</bk-button>
            <div class="mr20 flex-align-center">
                <bk-input
                    v-model.trim="name"
                    class="w250"
                    placeholder="请输入制品名称, 按Enter键搜索"
                    clearable
                    @enter="handlerPaginationChange()"
                    @clear="handlerPaginationChange()"
                    right-icon="bk-icon icon-search"
                />
            </div>
        </div>
        <!-- 筛选框 /-->

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
            <bk-table-column label="制品名称"></bk-table-column>
            <bk-table-column label="版本"></bk-table-column>
            <bk-table-column label="制品类型"></bk-table-column>
            <bk-table-column label="操作">
                <bk-button class="mr10" theme="primary" text>编辑</bk-button>
                <bk-button theme="primary" text>删除</bk-button>
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
        <add-artifact-dialog
            v-model="artifactDialog.addVisible"
            @close="handleClickShowDialog('add-close')"
            @confirm="handleClickAddConfirm"
        />
        <!-- 编辑制品信息dialog -->
        <edit-artifact-dialog
            v-model="artifactDialog.editVisible"
            @close="handleClickShowDialog('add-close')"
            @confirm="handleClickAddConfirm"
        />
    </div>
</template>

<script>
    import { mapActions } from 'vuex'
    import AddArtifactDialog from './proxyWhiteListDialog/addArtifactDialog.vue'
    import EditArtifactDialog from './proxyWhiteListDialog/editArtifactDialog.vue'
    export default {
        components: {
            AddArtifactDialog,
            EditArtifactDialog
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
                artifactDialog: {
                    addVisible: false,
                    editVisible: false
                },
                params: {
                    repositoryType: '',
                    packageKey: '',
                    version: '',
                    pageNumber: 1,
                    pageSize: 20
                }
            }
        },
        created () {
            this.getWhitelist(this.params).then(res => {
                console.log(res)
            })
        },
        methods: {
            ...mapActions([
                'getWhitelist'
            ]),
            handlerPaginationChange () {
                
            },
            // 显示dialog
            handleClickShowDialog (dialogType) {
                switch (dialogType) {
                    case 'add':
                        this.artifactDialog.addVisible = true
                        break
                    case 'add-close':
                        this.artifactDialog.addVisible = false
                        break
                    case 'edit':
                        
                        break
                    default:
                        break
                }
            },
            // 添加制品信息提交
            handleClickAddConfirm (formdata) {

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

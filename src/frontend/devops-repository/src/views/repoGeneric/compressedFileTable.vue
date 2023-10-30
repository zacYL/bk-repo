<template>
    <canway-dialog
        v-model="previewDialog.show"
        :title="previewDialog.title"
        :width="dialogWidth"
        height-num="705"
        @cancel="previewDialog.show = false">
        <breadcrumb style="height:20px;" :list="breadcrumb"></breadcrumb>
        <bk-table
            class="mt5"
            height="450"
            v-bkloading="{ isLoading }"
            :data="curPageData"
            @row-dblclick="openFolder">
            <bk-table-column :label="$t('fileName')" prop="name" show-overflow-tooltip>
                <template #default="{ row }">
                    <Icon class="table-svg" size="16" :name="row.folder ? 'folder' : getIconName(row.name)" />
                    <span class="ml10">{{row.name}}</span>
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('size')" width="90" show-overflow-tooltip>
                <template #default="{ row }">
                    {{ row.folder ? '/' : (row.size > -1 ? convertFileSize(row.size) : $t('unknownSize')) }}
                </template>
            </bk-table-column>
            <bk-table-column :label="$t('operation')" width="100">
                <template #default="{ row }">
                    <bk-button v-if="row.folder" text @click="openFolder(row)">{{$t('openBtn')}}</bk-button>
                    <div v-else v-bk-tooltips="{ disabled: handlerPreview(row), content: $t('supportPreview') }">
                        <bk-button text :disabled="!handlerPreview(row)" @click="handlerPreview(row, true)">
                            {{ $t('preview') }}
                        </bk-button>
                    </div>
                </template>
            </bk-table-column>
        </bk-table>
        <bk-pagination
            class="p10"
            size="small"
            align="right"
            :show-limit="false"
            :current.sync="pagination.current"
            :count="paginationCount">
        </bk-pagination>
        <template #footer>
            <bk-button theme="primary" @click="previewDialog.show = false">{{ $t('confirm') }}</bk-button>
        </template>
    </canway-dialog>
</template>

<script>
    import Breadcrumb from '@repository/components/Breadcrumb'
    import { convertFileSize } from '@repository/utils'
    import { mapActions } from 'vuex'
    import { getIconName } from '@repository/store/publicEnum'
    export default {
        name: 'compressedFileTable',
        components: { Breadcrumb },
        data () {
            return {
                isLoading: false,
                compressedData: [],
                selectedTreeNode: {},
                pagination: {
                    current: 1,
                    limit: 10
                },
                previewDialog: {
                    show: false,
                    title: '',
                    projectId: '',
                    repoName: '',
                    fullPath: ''
                },
                dialogWidth: window.innerWidth - 600
            }
        },
        computed: {
            curPageData () {
                if (!this.selectedTreeNode.children) return []
                return this.selectedTreeNode.children
                    .sort((a, b) => {
                        if (a.folder === b.folder) {
                            return a.name.charCodeAt() - b.name.charCodeAt()
                        } else {
                            return b.folder - a.folder
                        }
                    })
                    .slice(
                        (this.pagination.current - 1) * this.pagination.limit,
                        this.pagination.current * this.pagination.limit
                    )
            },
            paginationCount () {
                if (!this.selectedTreeNode.children) return 0
                return this.selectedTreeNode.children.length
            },
            breadcrumb () {
                if (!this.compressedData[0]) return []
                const breadcrumb = [{
                    name: this.previewDialog.title,
                    value: this.compressedData[0],
                    cilckHandler: item => {
                        this.openFolder(item.value)
                    }
                }]
                let node = this.compressedData[0].children
                const road = this.selectedTreeNode.filePath.split('/').filter(Boolean)
                road.forEach(name => {
                    const temp = node.find(o => o.name === name)
                    breadcrumb.push({
                        name,
                        value: temp,
                        cilckHandler: item => {
                            this.openFolder(item.value)
                        }
                    })
                    node = temp.children
                })
                return breadcrumb
            }
        },
        methods: {
            getIconName,
            convertFileSize,
            ...mapActions([
                'previewCompressedFileList'
            ]),
            setData (data) {
                const { show, projectId, repoName, fullPath } = data
                this.previewDialog = {
                    ...this.previewDialog,
                    ...data
                }
                if (!show) return
                this.isLoading = true
                this.previewCompressedFileList({
                    projectId,
                    repoName,
                    fullPath
                }).then(res => {
                    this.compressedData = this.createTreeData(res)
                    this.selectedTreeNode = this.compressedData[0]
                    this.pagination.current = 1
                }).finally(() => {
                    this.isLoading = false
                })
            },
            createTreeData (arrayData) {
                const rootNode = [{
                    name: this.previewDialog.title,
                    folder: true,
                    filePath: '',
                    children: []
                }]
                arrayData.forEach(file => {
                    let pointNode = rootNode[0].children
                    let index = 0
                    const names = file.name.split('/')
                    names.forEach(name => {
                        index++
                        let temp = pointNode.find(o => o.name === name)
                        if (!temp) {
                            if (index === names.length) {
                                temp = { name, filePath: file.name, folder: false, size: file.size }
                            } else {
                                temp = { name, children: [], filePath: names.slice(0, index).join('/'), folder: true }
                            }
                            pointNode.push(temp)
                        }
                        pointNode = temp.children
                    })
                })
                return rootNode
            },
            handlerPreview (row, excute = false) {
                const ext = row.name.replace(/^.+\.([^.]+)$/, '$1')
                const basicEnable = [
                    'txt', 'sh', 'bat', 'json', 'yaml', 'md',
                    'xml', 'log', 'ini', 'properties', 'toml'
                ].includes(ext)
                if (basicEnable) {
                    excute && this.$emit('show-preview', {
                        name: row.filePath,
                        projectId: this.previewDialog.projectId,
                        repoName: this.previewDialog.repoName,
                        fullPath: this.previewDialog.fullPath,
                        filePath: row.filePath
                    })
                    return true
                }
                return false
            },
            openFolder (row) {
                if (!row.folder) return
                this.selectedTreeNode = row
            }
        }
    }
</script>

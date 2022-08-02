<template>
    <div class="upload-viewport-container" :class="{ 'visible': show, 'file-visible': showFileList }">
        <div class="viewport-header flex-between-center">
            <div class="header-title">制品库上传任务</div>
            <div class="header-operation flex-align-center">
                <i v-if="showFileList" class="devops-icon icon-minus" @click="showFileList = !showFileList"></i>
                <Icon v-else size="14" name="icon-expand" @click.native="showFileList = !showFileList" />
                <i class="ml20 devops-icon icon-close" @click="closeViewport"></i>
            </div>
        </div>
        <div class="pt10 viewport-table" v-show="showFileList">
            <bk-table
                :data="fileList"
                height="100%"
                :outer-border="false"
                :row-border="false"
                size="small">
                <bk-table-column :label="$t('fileName')" show-overflow-tooltip>
                    <template #default="{ row }">
                        <bk-popover placement="top">
                            {{row.file.name}}
                            <template #content>
                                <div>项目：{{ (projectList.find(p => p.id === row.projectId)).name }}</div>
                                <div>仓库：{{ replaceRepoName(row.repoName) }}</div>
                                <div>文件存储路径：{{ row.fullPath }}</div>
                            </template>
                        </bk-popover>
                    </template>
                </bk-table-column>
                <bk-table-column label="状态" width="100">
                    <template #default="{ row }">
                        <span v-if="row.status === 'UPLOADING'"
                            v-bk-tooltips="{ content: row.progressDetail, placements: ['bottom'] }">
                            {{ row.progressPercent }}
                        </span>
                        <span v-else class="repo-tag" :class="row.status"
                            v-bk-tooltips="{ disabled: row.status !== 'FAILED' || !row.errMsg, content: row.errMsg, placements: ['bottom'] }">
                            {{ uploadStatus[row.status].label }}
                        </span>
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('operation')" width="70">
                    <template #default="{ row }">
                        <bk-button v-if="row.status === 'INIT' || row.status === 'UPLOADING'"
                            text theme="primary" @click="cancelUpload(row)">取消</bk-button>
                        <bk-button v-else-if="row.status === 'SUCCESS'"
                            text theme="primary" @click="$router.push({
                                name: 'repoGeneric',
                                params: { projectId: row.projectId },
                                query: { repoName: row.repoName, path: row.fullPath }
                            })">查看</bk-button>
                        <bk-button v-else-if="row.status === 'CANCEL' || row.status === 'FAILED'"
                            text theme="primary" @click="reUpload(row)">上传</bk-button>
                    </template>
                </bk-table-column>
            </bk-table>
        </div>
        <selected-files-dialog
            ref="selectedFilesDialog"
            :root-data="rootData"
            @confirm="addFilesToFileList">
        </selected-files-dialog>
    </div>
</template>
<script>
    import Vue from 'vue'
    import selectedFilesDialog from './selectedFilesDialog'
    import { mapState, mapActions } from 'vuex'
    import { convertFileSize } from '@repository/utils'
    const uploadStatus = {
        UPLOADING: { label: '正在上传', power: 1 },
        INIT: { label: '等待上传', power: 2 },
        FAILED: { label: '上传失败', power: 3 },
        CANCEL: { label: '已取消', power: 4 },
        SUCCESS: { label: '上传完成', power: 5 }
    }
    export default {
        name: 'globalUploadViewport',
        components: { selectedFilesDialog },
        data () {
            return {
                uploadStatus,
                show: false,
                showFileList: true,
                rootData: {
                    projectId: '',
                    repoName: '',
                    folder: false,
                    fullPath: ''
                },
                fileList: [],
                upLoadTaskQueue: []
            }
        },
        computed: {
            ...mapState(['projectList'])
        },
        mounted () {
            Vue.prototype.$globalUploadFiles = this.selectFiles
        },
        methods: {
            ...mapActions([
                'uploadArtifactory'
            ]),
            selectFiles (data = {}) {
                this.rootData = {
                    ...this.rootData,
                    ...data
                }
                this.$refs.selectedFilesDialog.selectFiles()
            },
            addFilesToFileList ({ overwrite, selectedFiles }) {
                const fileList = selectedFiles.map(file => this.getUploadObj(file, overwrite))
                this.sortFileList(fileList)
                this.addToUpLoadTaskQueue()
                this.show = true
            },
            getUploadObj (file, overwrite) {
                const { projectId, repoName, fullPath: path } = this.rootData
                // TODO
                const fullPath = `${path}/${this.rootData.folder ? file.webkitRelativePath : file.name}`
                return {
                    xhr: new XMLHttpRequest(),
                    projectId,
                    repoName,
                    fullPath,
                    file,
                    overwrite,
                    status: 'INIT'
                }
            },
            sortFileList (extFiles = []) {
                this.fileList = [...this.fileList, ...extFiles].sort((a, b) => {
                    return uploadStatus[a.status].power - uploadStatus[b.status].power
                })
            },
            closeViewport () {
                this.$confirm({
                    theme: 'danger',
                    message: '确认 取消所有上传任务并清空任务列表 ？',
                    confirmFn: () => {
                        this.show = false
                        this.showFileList = true
                        this.fileList.forEach(this.cancelUpload)
                        this.fileList = []
                    }
                })
            },
            addToUpLoadTaskQueue () {
                const wait = this.fileList.find(f => f.status === 'INIT')
                if (this.upLoadTaskQueue.length > 5 || !wait) return
                this.$set(wait, 'status', 'UPLOADING')
                const { xhr, projectId, repoName, fullPath, file, overwrite, status } = wait
                this.uploadArtifactory({
                    xhr,
                    projectId,
                    repoName,
                    fullPath,
                    body: file,
                    progressHandler: ($event) => {
                        const { progressDetail, progressPercent } = this.getProgress($event)
                        this.$set(wait, 'progressDetail', progressDetail)
                        this.$set(wait, 'progressPercent', progressPercent)
                    },
                    headers: {
                        'Content-Type': file.type || 'application/octet-stream',
                        'X-BKREPO-OVERWRITE': overwrite,
                        'X-BKREPO-EXPIRES': 0
                    }
                }).then(() => {
                    this.$set(wait, 'status', 'SUCCESS')
                    window.repositoryVue.$emit('upload-refresh', fullPath)
                }).catch(e => {
                    if (status === 'CANCEL') return
                    e && this.$set(wait, 'errMsg', e.message || e)
                    this.$set(wait, 'status', 'FAILED')
                }).finally(() => {
                    this.upLoadTaskQueue = this.upLoadTaskQueue.filter(task => task !== wait)
                    this.addToUpLoadTaskQueue()
                    this.sortFileList()
                })
                this.upLoadTaskQueue.push(wait)
                this.addToUpLoadTaskQueue()
            },
            cancelUpload (row) {
                if (row.status === 'UPLOADING') row.xhr.abort() // 取消走catch分支
                this.$set(row, 'status', 'CANCEL')
            },
            reUpload (row) {
                this.$set(row, 'status', 'INIT')
                this.sortFileList()
                this.addToUpLoadTaskQueue() // 开启队列
            },
            getProgress ({ loaded, total }) {
                const progressDetail = `(${convertFileSize(loaded)}/${convertFileSize(total)})`
                const progressPercent = parseInt(100 * loaded / total) + '%'
                return { progressDetail, progressPercent }
            }
        }
    }
</script>
<style lang="scss" scoped>
.upload-viewport-container {
    display: none;
    position: fixed;
    right: 40px;
    bottom: 60px;
    width: 520px;
    z-index: 1999;
    border-radius: 3px;
    box-shadow: 0px 0px 20px 0px rgba(8, 30, 64, 0.2);
    &.visible {
        display: initial;
    }
    .viewport-header {
        height: 50px;
        padding: 0 20px;
        background-color: var(--bgHoverColor);
        .header-title {
            font-size: 14px;
            font-weight: 500;
        }
        .header-operation {
            color: var(--fontSubsidiaryColor);
            svg,
            .devops-icon {
                cursor: pointer;
            }
        }
    }
    .viewport-table {
        height: 325px;
        border-top: 1px solid var(--borderColor);
        background-color: white;
    }
    &.file-visible {
        .viewport-header {
            background-color: white;
        }
    }
}
</style>

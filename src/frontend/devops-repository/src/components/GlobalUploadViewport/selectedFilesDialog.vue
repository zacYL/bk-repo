<template>
    <canway-dialog
        v-model="show"
        width="600"
        height-num="450"
        @cancel="cancel">
        <template #header>
            <div class="flex-align-center">
                <span class="mr10 canway-dialog-title">{{$t('uploadFile')}}</span>
                <span class="repo-tag" v-bk-tooltips=" { content: rootData.fullPath || '/', placements: ['bottom'] ,disabled: !rootData.fullPath }">{{ rootData.fullPath || '/' }}</span>
            </div>
        </template>
        <div class="flex-between-center">
            <bk-button @click="selectFiles(false)">{{$t('continueUploading')}}</bk-button>
            <div class="flex-align-center" v-if="!rootData.uploadType">
                <label style="white-space:nowrap;">{{$t('fileSameNameOverwrites')}}: </label>
                <bk-radio-group v-model="overwrite">
                    <bk-radio class="ml20" :value="true">{{ $t('allow') }}</bk-radio>
                    <bk-radio class="ml20" :value="false">{{ $t('notAllow') }}</bk-radio>
                </bk-radio-group>
            </div>
        </div>
        <div style="height:240px"
            v-bkLoading="{ isLoading: refreshTable }">
            <bk-table
                v-if="show && !refreshTable"
                class="mt10"
                :data="selectedFiles"
                height="100%"
                :outer-border="false"
                :row-border="false"
                :virtual-render="selectedFiles.length > 3000"
                size="small">
                <bk-table-column :label="$t('fileName')" prop="name" show-overflow-tooltip min-width="200">
                    <template #default="{ row }">
                        <div class="flex-align-center">
                            <!-- maven 上传内容异常时，会出现提示 -->
                            <template v-if="(rootData.uploadType === 'mavenUpload' && ((!row._tempParams) || (row._tempParams && checkCoordinateEmpty(row._tempParams))))">
                                <Icon class="mr5" name="info" size="12" style="color: #ea3736;" v-bk-tooltips="row?._errorMsg || $t('gavErrTips')" />
                            </template>
                            {{ row.name }}
                        </div>
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('size')" width="90" show-overflow-tooltip>
                    <template #default="{ row }">{{ convertFileSize(row.size) }}</template>
                </bk-table-column>
                <bk-table-column :label="$t('operation')" min-width="100">
                    <template #default="{ row, $index }">
                        <!-- 如果坐标信息不存在，或者坐标核心信息存在空的情况 -->
                        <bk-button
                            text
                            theme="primary"
                            @click="selectedFiles.splice($index, 1)">
                            {{$t('remove')}}
                        </bk-button>

                        <!-- maven 批量上传 支持坐标查看，重新上传 -->
                        <template v-if="rootData.uploadType === 'mavenUpload'">
                            <!-- 坐标查看按钮 -->
                            <bk-button
                                text
                                theme="primary"
                                class="ml5"
                                v-bk-tooltips="{
                                    content: `<div>
                                                <p>Group ID：${row?._tempParams?.groupId || ''}</p>
                                                <p>Artifact ID: ${row?._tempParams?.artifactId || ''}</p>
                                                <p>Version: ${row?._tempParams?.version || ''}</p>
                                            </div>`,
                                    width: 300,
                                    placements: ['right']
                                }">
                                {{$t('view')}}
                            </bk-button>

                            <!-- 重新上传按钮 -->
                            <!-- 如果坐标信息不存在，或者坐标核心信息存在空的情况，且非pom文件的情况下 -->
                            <bk-button
                                v-if="((!row._tempParams) || (row._tempParams && checkCoordinateEmpty(row._tempParams))) && !row.name.endsWith('.pom')"
                                text
                                theme="primary"
                                class="ml5"
                                @click="reupload($index)">
                                {{$t('upload') + $t('space') + 'pom'}}
                            </bk-button>
                        </template>
                    </template>
                </bk-table-column>
            </bk-table>
        </div>
        
        <input
            class="upload-input"
            key="uploadInput"
            ref="uploadInput"
            type="file"
            v-bind="rootData.uploadType === 'mavenUpload'
                ? {
                    accept: '.pom,.jar,.war,.tar,.ear,.ejb,.rar,.msi,.aar,.kar,.rpm,.tar.bz2,.tar.gz,.tar.xz,.tbz,.zip'
                }
                : {}"
            :webkitdirectory="rootData.folder"
            @change="selectedFilesHandler"
            multiple
        />
        <!-- 重新上传文件input，单选只能.pom后缀 -->
        <input class="upload-input" key="reuploadInput" ref="reuploadInput" type="file" :webkitdirectory="rootData.folder" @change="reselectedFilesHandler" accept=".pom">
        <template #footer>
            <bk-button @click="cancel">{{ $t('cancel') }}</bk-button>
            <bk-button class="ml10" theme="primary" @click="confirm">{{ $t('confirm') }}</bk-button>
        </template>
    </canway-dialog>
</template>
<script>
    import { mapActions } from 'vuex'
    import { convertFileSize } from '@repository/utils'
    export default {
        name: 'selectedFilesDialog',
        props: {
            rootData: Object
        },
        data () {
            return {
                reuploadIndex: -1,
                show: false,
                refreshTable: false,
                overwrite: false,
                selectedFiles: []
            }
        },
        methods: {
            ...mapActions([
                'uploadArtifactory'
            ]),
            convertFileSize,
            selectFiles (isReupload = false) {
                const refKey = isReupload ? 'reuploadInput' : 'uploadInput'
                this.$refs[refKey].value = ''
                this.$nextTick(() => {
                    this.$refs[refKey].click()
                })
            },
            // 重新上传，如果上传异常，支持重新上传
            reupload (index) {
                // 设置重写坐标
                this.reuploadIndex = index
                this.selectFiles(true)
            },
            /**
             * @description: 重新选择文件，处理函数，同名会被过滤
             * @param {*}
             * @return {*}
             */
            reselectedFilesHandler () {
                const file = this.$refs.reuploadInput.files[0]
                if (!file) {
                    return this.$bkMessage({
                        message: this.$t('selectFileTip'),
                        theme: 'error'
                    })
                }
                this.refreshTable = true
                this.handleUpload(file, 'pomUpload').then((res) => {
                    // 更新坐标
                    const { uuid: existingUuid } = this.selectedFiles[this.reuploadIndex]._tempParams || {}
                    this.selectedFiles[this.reuploadIndex]._tempParams = {
                        ...res.value,
                        uuid: existingUuid || res.value.uuid || ''
                    }
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('uploadSuccess')
                    })
                }).catch(() => {
                    this.$bkMessage({
                        message: this.$t('uploadMavenErrorMsgTip'),
                        theme: 'error'
                    })
                }).finally(() => {
                    this.reuploadIndex = -1
                    this.refreshTable = false
                })
            },
            selectedFilesHandler () {
                const files = [...this.$refs.uploadInput.files]
                if (!files.length) return
                const tmlSelectedFiles = []
                files.forEach(file => {
                    const insert = !this.selectedFiles.find(f => {
                        return this.rootData.folder
                            ? f.webkitRelativePath === file.webkitRelativePath
                            : f.name === file.name
                    })
                    if (insert) {
                        (tmlSelectedFiles.push(file))
                    }
                })
                if (this.rootData.uploadType && this.rootData.uploadType === 'mavenUpload') {
                    // 生成上传任务Promise数组
                    const uploaderPromises = tmlSelectedFiles.map(file => {
                        return this.handleUpload(file, this.rootData.uploadType)
                    })
                    Promise.allSettled(uploaderPromises).then(res => {
                        res.forEach(v => {
                            const value = v.value || {}
                            const reason = v.reason || {}
                            if (v.status === 'fulfilled') {
                                this.$set(value.file, '_tempParams', value.value)
                                this.selectedFiles.push(value.file)
                            } else {
                                this.$set(reason.file, '_errorMsg', reason.error)
                                this.selectedFiles.push(reason.file)
                            }
                        })
                        this.show = true
                    })
                } else {
                    this.selectedFiles.push(...tmlSelectedFiles)
                    this.show = true
                }
            },
            // 坐标空校验
            checkCoordinateEmpty (data) {
                return (!data.groupId || !data.artifactId || !data.version)
            },
            // 坐标全空校验
            checkCoordinate (data) {
                return (!data.groupId && !data.artifactId && !data.version)
            },
            // 上传过滤函数，解析后会得到相关坐标信息，目前这里只处理了maven上传
            handleUpload (uploadFile, uploadType) {
                return new Promise((resolve, reject) => {
                    const uploadXhr = new XMLHttpRequest()
                    const body = uploadFile
                    const formData = new FormData()
                    formData.append('file', body)
                    const headers = {
                        'Content-Type': 'multipart/form-data; boundary=----',
                        'X-BKREPO-EXPIRES': 0
                    }

                    this.uploadArtifactory({
                        xhr: uploadXhr,
                        body: formData,
                        headers,
                        uploadType
                    }).then(res => {
                        if (['mavenUpload', 'pomUpload'].includes(uploadType)) {
                            if ((!res || !res.data || this.checkCoordinate(res.data))) {
                                resolve({
                                    value: {
                                        uuid: res.data.uuid || '',
                                        groupId: '',
                                        artifactId: '',
                                        version: '',
                                        classifier: '',
                                        type: body.name.split('.').pop()
                                    },
                                    file: uploadFile
                                })
                            } else {
                                resolve({
                                    value: res.data,
                                    file: uploadFile
                                })
                            }
                        }
                    }).catch((error) => {
                        // eslint-disable-next-line prefer-promise-reject-errors
                        reject({
                            error: error.message,
                            file: uploadFile
                        })
                    })
                })
            },
            confirm () {
                let submitFiles = []
                if (this.rootData?.uploadType === 'mavenUpload') {
                    submitFiles = this.selectedFiles.filter(file => {
                        return !this.checkCoordinateEmpty(file._tempParams || {})
                    })
                } else {
                    submitFiles = this.selectedFiles
                }
                if (submitFiles.length) {
                    this.$emit('confirm', {
                        overwrite: this.overwrite,
                        selectedFiles: submitFiles
                    })
                    this.cancel()
                } else {
                    this.$bkMessage({
                        message: this.$t('请选择合法文件'),
                        theme: 'error'
                    })
                }
            },
            cancel () {
                this.show = false
                this.overwrite = false
                this.selectedFiles = []
            }
        }
    }
</script>
<style lang="scss" scoped>
.upload-input {
    display: none;
}
</style>

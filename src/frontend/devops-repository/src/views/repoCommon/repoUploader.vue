<template>
    <bk-sideslider
        :is-show.sync="isVisible"
        :quick-close="true"
        :width="500"
        @hidden="handleClickClose"
    >
        <div slot="header">{{ customSettings.title }}</div>
        <div class="content-container" slot="content">
            <div v-if="customSettings.uploadFlag">
                <div class="content-info">
                    <p v-if="repoType === 'MAVEN'">{{ $t('selectMavenArtifact') }}</p>
                    <p v-if="repoType === 'NPM'">{{ $t('selectNpmArtifact') }}</p>
                    <p v-if="repoType === 'DOCKER'">{{ $t('selectDockerImage') }}</p>
                </div>
                <bk-upload
                    v-bkloading="{ isLoading: isLoading, title: $t('uploadMavenArtifactLoading') }"
                    :with-credentials="true"
                    :size="5 * 1024"
                    :limit="1"
                    :multiple="false"
                    v-bind="{
                        ...repoType === 'MAVEN' ? { accept: '.pom,.jar,.war,.tar,.ear,.ejb,.rar,.msi,.aar,.kar,.rpm,.tar.bz2,.tar.gz,.tar.xz,.tbz,.zip' } : {}
                    }"
                    :custom-request="onRequestUpload"
                    ext-cls="content-upload"
                    url="#"
                />
                <template v-if="repoType === 'NPM'">
                    <bk-form
                        class="mt10"
                        ref="formRef"
                        :label-width="200"
                        :model="npmFormData"
                        form-type="vertical"
                    >
                        <bk-form-item :label="$t('currentFile')" v-if="npmFormData.currentFile" required>
                            <bk-input disabled v-model.trim="npmFormData.currentFile" />
                        </bk-form-item>
                    </bk-form>
                    <div class="g-flex mt10">
                        {{ $t('progress') }}:
                        <bk-progress :stroke-width="8" :percent="uploadPercent" />
                    </div>
                </template>
                <template v-if="repoType === 'DOCKER'">
                    <bk-form
                        class="mt10"
                        ref="formRef"
                        :label-width="200"
                        :model="dockerFormData"
                        form-type="vertical"
                        :rules="dockerRules"
                    >
                        <bk-form-item :label="$t('currentFile')" v-if="dockerFormData.currentFile" required>
                            <bk-input disabled v-model.trim="dockerFormData.currentFile" />
                        </bk-form-item>
                        <bk-form-item label="Package Name" property="packageName" required>
                            <bk-input v-model.trim="dockerFormData.packageName" />
                        </bk-form-item>
                        <bk-form-item label="Version" property="version" required>
                            <bk-input v-model.trim="dockerFormData.version" />
                        </bk-form-item>
                    </bk-form>
                    <div class="g-flex mt10">
                        {{ $t('progress') }}:
                        <bk-progress :stroke-width="8" :percent="uploadPercent" />
                    </div>
                </template>
                <div class="upload-show-file-container" v-if="currentFileName">
                    <div class="upload-show-file-container-info">
                        {{ currentFileName || '' }}
                        <bk-progress :stroke-width="8" :percent="uploadPercent" />
                    </div>
                    <bk-button
                        v-if="uploadPercent > 0 && uploadPercent !== 1"
                        class="upload-show-file-container-cancel"
                        :title="$t('cancel')"
                        theme="warning"
                        :text="true"
                        @click="onAbortUpload"
                    >
                        {{ $t('cancel') }}
                    </bk-button>
                    <bk-button
                        v-if="uploadPercent === 1 && !errorMsg && !isLoading"
                        class="upload-show-file-container-cancel"
                        :title="$t('uploadMavenSuccessInfo')"
                        theme="success"
                        :text="true"
                    >
                        {{ $t('uploadMavenSuccessInfo') }}
                    </bk-button>
                </div>
                <div class="error-upload-info" v-if="errorMsg">{{ errorMsg || $t('uploadMavenErrorMsg') }}</div>
            </div>
            <div v-else>
                <template v-if="repoType === 'MAVEN'">
                    <div class="content-file-info" v-if="currentFileName">
                        <span>{{ currentFileName }}</span>
                        <bk-icon-plus type="plus-close" @click="() => {
                            currentFileName = ''
                        }" />
                    </div>
                    <bk-form
                        ref="formRef"
                        :label-width="200"
                        :model="mavenFormData"
                        form-type="vertical"
                        :rules="mavenRules"
                    >
                        <bk-form-item label="Group ID" property="groupId" :required="checkFail">
                            <bk-input :readonly="!checkFail" v-model.trim="mavenFormData.groupId" />
                        </bk-form-item>
                        <bk-form-item label="Artifact ID" property="artifactId" :required="checkFail">
                            <bk-input :readonly="!checkFail" v-model.trim="mavenFormData.artifactId" />
                        </bk-form-item>
                        <bk-form-item label="Version" property="version" :required="checkFail">
                            <bk-input :readonly="!checkFail" v-model.trim="mavenFormData.version" />
                        </bk-form-item>
                        <bk-form-item v-if="mavenFormData.classifier" label="Classifier" property="classifier">
                            <bk-input readonly v-model.trim="mavenFormData.classifier" />
                        </bk-form-item>
                        <bk-form-item label="Type" property="type">
                            <bk-input readonly v-model.trim="mavenFormData.type" />
                        </bk-form-item>
                        <bk-form-item v-if="checkFail">
                            <bk-button
                                text
                                theme="primary"
                                class="ml5"
                                @click="reupload()"
                            >
                                {{ $t('upload') + $t('space') + 'pom' }}
                            </bk-button>
                            <bk-upload
                                v-show="false"
                                ref="pomUpload"
                                class="mb5"
                                v-bkloading="{ isLoading: isLoading, title: $t('uploadMavenArtifactLoading') }"
                                :with-credentials="true"
                                :size="5 * 1024"
                                :limit="1"
                                :multiple="false"
                                :custom-request="onRequestPomUpload"
                                :accept="'.pom'"
                                url="#"
                            />
                        </bk-form-item>
                    </bk-form>
                </template>
            </div>
        </div>
        <div slot="footer">
            <bk-button @click.stop.prevent="btnCancel">{{ $t('cancel') }}</bk-button>
            <bk-button
                class="ml10"
                theme="primary"
                v-bind="{
                    ...repoType === 'MAVEN' ? {
                        disabled: customSettings.uploadFlag || customSettings.saveBtnDisable
                    } : {},
                    ...repoType === 'DOCKER' ? {
                        disabled: customSettings.saveBtnDisable
                    } : {}
                }"
                @click.stop.prevent="submitData"
            >
                {{ $t('confirm') }}
            </bk-button>
        </div>
    </bk-sideslider>
</template>

<script>
    import { mapActions } from 'vuex'

    export default {
        model: {
            prop: 'isVisible',
            event: 'update'
        },
        props: {
            repoType: {
                type: String,
                default: 'MAVEN'
            },
            isVisible: {
                type: Boolean,
                default: false
            },
            projectId: {
                type: String,
                default: ''
            },
            repoName: {
                type: String,
                default: ''
            }
        },
        data () {
            return {
                mavenRules: {
                    groupId: [
                        {
                            required: true,
                            message: this.$t('pleaseInput') + this.$t('space') + 'groupID',
                            trigger: 'blur'
                        },
                        {
                            regex: /^[a-zA-Z0-9._-]+(?<!\.)$/,
                            message: this.$t('fieldDescription1'),
                            trigger: 'blur'
                        },
                        {
                            validator: (val) => {
                                return !(val + '').startsWith('.')
                            },
                            message: this.$t('fieldDescription1'),
                            trigger: 'blur'
                        }
                    ],
                    version: [
                        {
                            required: true,
                            message: this.$t('pleaseInput') + this.$t('space') + 'Version',
                            trigger: 'blur'
                        },
                        {
                            regex: /^[a-zA-Z0-9._-]+(?<!\.)$/,
                            message: this.$t('fieldDescription1'),
                            trigger: 'blur'
                        },
                        {
                            validator: (val) => {
                                return !(val + '').startsWith('.')
                            },
                            message: this.$t('fieldDescription1'),
                            trigger: 'blur'
                        }
                    ],
                    artifactId: [
                        {
                            required: true,
                            message: this.$t('pleaseInput') + this.$t('space') + 'artifactID',
                            trigger: 'blur'
                        },
                        {
                            regex: /^[a-zA-Z0-9._-]+(?<!\.)$/,
                            message: this.$t('fieldDescription1'),
                            trigger: 'blur'
                        },
                        {
                            validator: (val) => {
                                return !(val + '').startsWith('.')
                            },
                            message: this.$t('fieldDescription1'),
                            trigger: 'blur'
                        }
                    ]
                },
                dockerRules: {
                    packageName: [
                        {
                            required: true,
                            message: this.$t('pleaseInput') + this.$t('space') + 'Package Name',
                            trigger: 'blur'
                        },
                        {
                            regex: /^[a-z0-9]+(([_.]|__|-*)[a-z0-9]+)*$/,
                            message: this.$t('checkRegexp', ['[a-z0-9]+(([_.]|__|-*)[a-z0-9]+)*']),
                            trigger: 'blur'
                        }
                    ],
                    version: [
                        {
                            required: true,
                            message: this.$t('pleaseInput') + this.$t('space') + 'Version',
                            trigger: 'blur'
                        },
                        {
                            regex: /^\w[\w.-]{0,127}$/,
                            message: this.$t('checkRegexp', ['\\w[\\w.-]{0,127}']),
                            trigger: 'blur'
                        }
                    ]
                },
                customSettings: {
                    isShow: false,
                    title: this.$t('uploadMavenConfirmContent'),
                    uploadFlag: true, // 是否显示上传组件
                    saveBtnDisable: false // 确认按钮是否禁用
                },
                mavenFormData: {
                    uuid: '',
                    groupId: '',
                    artifactId: '',
                    version: '',
                    classifier: '',
                    type: ''
                },
                dockerFormData: {
                    packageName: '',
                    version: '',
                    file: '',
                    currentFile: ''
                },
                npmFormData: {
                    file: '',
                    currentFile: ''
                },
                checkFail: false, // 用于上传校验，如果失败的话，提供pom文件上传按钮
                currentFileName: '', // 当前上传的文件名
                errorMsg: '', // 上传接口后台返回的错误信息
                uploadPercent: 0,
                uploadXhr: null, // 上传请求的xhr对象，用于上传和取消上传时所用
                isLoading: false // 上传选择框是否在加载中状态，因为不能禁用，所以选择使用loading代替
            }
        },
        watch: {
            isVisible (val) {
                if (val) {
                    this.checkFail = false
                }
            }
        },
        beforeDestroy () {
            if (this.uploadPercent !== 0 && this.uploadPercent !== 100) {
                this.$bkMessage({
                    theme: 'error',
                    message: this.$t('uploadCancel')
                })
                this.uploadXhr && this.uploadXhr.abort()
            }
        },
        methods: {
            ...mapActions([
                'uploadArtifactory',
                'submitMavenArtifactory',
                'deleteUselessPackage'
            ]),
            handleClickClose () {
                this.customSettings = {
                    isShow: false,
                    title: this.$t('uploadMavenConfirmContent'),
                    uploadFlag: true, // 是否显示上传组件
                    saveBtnDisable: false // 确认按钮是否禁用
                }
                // 关闭弹窗时重置滚动条需要的数据
                this.onAbortUpload()
                this.cancel()
            },
            onRequestPomUpload (uploadFile) {
                this.handleMavenUpload(uploadFile, 'pomUpload')
            },

            onRequestUpload (uploadFile) {
                if (this.repoType === 'MAVEN') this.handleMavenUpload(uploadFile, 'mavenUpload')
                if (this.repoType === 'DOCKER') this.handleDockerUpload(uploadFile)
                if (this.repoType === 'NPM') this.handleNpmUpload(uploadFile)
            },

            reupload () {
                this.$refs.pomUpload.fileList = []
                this.$nextTick(() => {
                    this.$refs.pomUpload.$refs.uploadel.click()
                })
            },

            handleNpmUpload (uploadFile) {
                this.npmFormData.file = uploadFile
                this.npmFormData.currentFile = uploadFile.fileObj.name
            },

            handleDockerUpload (uploadFile) {
                this.dockerFormData.file = uploadFile
                this.dockerFormData.currentFile = uploadFile.fileObj.name
            },

            handleMavenUpload (uploadFile, uploadType) {
                if (uploadType === 'mavenUpload') this.checkFail = false
                this.isLoading = true
                this.errorMsg = ''
                this.uploadXhr = new XMLHttpRequest()

                const body = uploadFile.fileList[0].origin
                const formData = new FormData()
                formData.append('file', body)
                const headers = {
                    'Content-Type': 'multipart/form-data; boundary=----',
                    'X-BKREPO-EXPIRES': 0
                }

                this.uploadArtifactory({
                    xhr: this.uploadXhr,
                    body: formData,
                    progressHandler: ($event) => {
                        const num = $event.loaded / $event.total
                        uploadFile.onProgress({ percent: num })
                        this.uploadPercent = num
                        this.currentFileName = body.name || ''
                    },
                    headers,
                    uploadType
                }).then(res => {
                    this.customSettings.uploadFlag = false
                    if (uploadType === 'mavenUpload' && (!res || !res.data || (!res.data.groupId && !res.data.artifactId && !res.data.version))) {
                        if (uploadType === 'mavenUpload') this.checkFail = true
                        this.mavenFormData = {
                            uuid: res.data.uuid || '',
                            groupId: '',
                            artifactId: '',
                            version: '',
                            classifier: '',
                            type: body.name.split('.').pop()
                        }
                    } else {
                        this.mavenFormData = {
                            ...res.data,
                            uuid: this.mavenFormData.uuid || res.data.uuid
                        }
                    }
                }).catch(error => {
                    if (uploadType === 'pomUpload') {
                        this.$bkMessage({
                            theme: 'error',
                            message: error?.message || this.$t('fileUploadErrorInfo')
                        })
                    } else {
                        // error &&  this.errorMsg = error.message || error.error || '无法识别包信息，请确认是否由Maven客户端打包，并重新上传'
                        this.errorMsg = error?.message || error?.error || error || this.$t('fileUploadErrorInfo')
                    }
                }).finally(() => {
                    this.isLoading = false
                })
            },
            // 取消上传操作(此时还没有解析文件)
            onAbortUpload () {
                this.isLoading = false
                this.uploadXhr && this.uploadXhr.abort()
                this.uploadXhr = null
                this.uploadPercent = 0
                this.currentFileName = ''
                this.errorMsg = ''
            },
            btnCancel () {
                this.onAbortUpload()
                this.cancel()
            },
            cancel () {
                this.$emit('cancel', false)
            },
            submitData () {
                const submitFn = () => {
                    this.uploadPercent = 0
                    this.errorMsg = ''
                    const submit = () => {
                        return new Promise((resolve, reject) => {
                            let params
                            const formData = new FormData()
                            const headers = {
                                'Content-Type': 'multipart/form-data; boundary=----',
                                'X-BKREPO-EXPIRES': 0
                            }
                            this.uploadXhr = new XMLHttpRequest()
                            const defaultUploaderPromise = (
                                uploadType, formData, file
                            ) => {
                                return this.uploadArtifactory({
                                    xhr: this.uploadXhr,
                                    body: formData,
                                    headers,
                                    uploadType,
                                    projectId: this.projectId,
                                    repoName: this.repoName,
                                    progressHandler: ($event) => {
                                        const num = $event.loaded / $event.total
                                        file.onProgress({ percent: num })
                                        this.uploadPercent = num
                                    }
                                })
                            }

                            switch (this.repoType) {
                                case 'MAVEN':
                                    params = {
                                        projectId: this.projectId,
                                        repoName: this.repoName,
                                        body: { ...this.mavenFormData }
                                    }
                                    this.submitMavenArtifactory(params)
                                        .then(res => {
                                            this.$emit('update', false)
                                            resolve()
                                        })
                                        .catch(error => {
                                            this.$bkMessage({
                                                theme: 'error',
                                                message: `${error?.errors?.[0]?.message || error.message || this.$t('uploadMavenErrorMsgTip')}`
                                            })
                                            reject(error)
                                        })
                                    break
                                case 'DOCKER':
                                    if (!this.dockerFormData.file) {
                                        reject(this.$t('uploadDockerErrorMsgTip'))
                                        break
                                    }
                                    formData.append('file', this.dockerFormData.file.fileList[0].origin)
                                    formData.append('version', this.dockerFormData.version)
                                    formData.append('packageName', this.dockerFormData.packageName)
                                    defaultUploaderPromise('dockerUpload', formData, this.dockerFormData.file)
                                        .then(res => {
                                            this.$bkMessage({
                                                theme: 'success',
                                                message: this.$t('uploadSuccess')
                                            })
                                            this.$emit('update', false)
                                            resolve()
                                        })
                                        .catch(error => {
                                            this.errorMsg = error?.errors?.[0]?.message || error.message || error.error || error
                                            reject(this.errorMsg)
                                        })
                                    break
                                case 'NPM':
                                    if (!this.npmFormData.file) {
                                        reject(this.$t('selectNpmArtifact'))
                                        break
                                    }
                                    formData.append('file', this.npmFormData.file.fileList[0].origin)
                                    defaultUploaderPromise('npmUpload', formData, this.npmFormData.file)
                                        .then(res => {
                                            this.$bkMessage({
                                                theme: 'success',
                                                message: this.$t('uploadSuccess')
                                            })
                                            this.$emit('update', false)
                                            resolve()
                                        })
                                        .catch(error => {
                                            this.errorMsg = error?.errors?.[0]?.message || error.message || error.error || error
                                            reject(this.errorMsg)
                                        })
                                    break

                                default:
                                    reject(this.$t('uploadFailed'))
                            }
                        })
                    }

                    this.customSettings.saveBtnDisable = true
                    submit()
                        .catch(err => {
                            this.$bkMessage({
                                theme: 'error',
                                message: err
                            })
                        })
                        .finally(() => {
                            this.customSettings.saveBtnDisable = false
                        })
                }
                // 如果表单存在，先校验
                if (this.$refs.formRef) {
                    this.$refs.formRef.validate().then(() => {
                        submitFn()
                    })
                } else {
                    submitFn()
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
.content-container {
    width: 100%;
    padding: 20px 30px;
}

.content-info {
    margin: 0 0 9px 0;
    font-style: normal;
    font-weight: 400;
    font-size: 12px;
    line-height: 16px;
    color: #081E40;
}

.content-upload {
    height: 181px;
}

.error-upload-info {
    color: red;
}

.content-file-info {
    box-sizing: border-box;
    width: 440px;
    height: 40px;
    background: rgba(58, 132, 255, 0.08);
    border-radius: 2px;
    padding: 0 10px;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.upload-show-file-container {
    height: 70px;
    padding: 5px;
    display: flex;
    justify-content: space-between;
    width: 100%;

    &-info {
        text-align: center;
        width: 78%;
    }

    &-cancel {
        margin: 22px 0 0 0;
    }
}

::v-deep .progress-text {
    margin: 0 0 0 10px;
}

::v-deep .bk-upload.draggable .file-wrapper {
    height: 100%;
}

::v-deep .bk-sideslider-footer {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    padding: 0 30px 0 0;
}

::v-deep .all-file {
    display: none;
}
</style>

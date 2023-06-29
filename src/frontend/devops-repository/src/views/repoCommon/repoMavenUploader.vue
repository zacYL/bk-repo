<template>
    <bk-sideslider
        :is-show.sync="isVisible"
        :quick-close="true"
        :width="500"
        @hidden="handleClickClose">
        <div slot="header">{{ customSettings.title }}</div>
        <div class="content-container" slot="content">
            <div v-if="customSettings.uploadFlag">
                <div class="content-info">
                    请选择Maven制品包
                </div>
                <bk-upload
                    v-bkloading="{ isLoading: isLoading, title: '文件正在解析中，请稍候' }"
                    :with-credentials="true"
                    :size="5 * 1024"
                    :limit="1"
                    :multiple="false"
                    :custom-request="onRequestUpload"
                    ext-cls="content-upload"
                    url="#"
                ></bk-upload>
                <div class="upload-show-file-container" v-if="currentFileName">
                    <div class="upload-show-file-container-info">
                        {{currentFileName || ''}}
                        <bk-progress :stroke-width="8" :percent="uploadPercent"></bk-progress>
                    </div>
                    <bk-button v-if="uploadPercent > 0 && uploadPercent !== 1" class="upload-show-file-container-cancel" title="取消" theme="warning" :text="true" @click="onAbortUpload">
                        取消
                    </bk-button>
                    <bk-button v-if="uploadPercent === 1 && !errorMsg && !isLoading" class="upload-show-file-container-cancel" title="文件解析成功" theme="success" :text="true">
                        文件解析成功
                    </bk-button>
                </div>
                <div class="error-upload-info" v-if="errorMsg">{{errorMsg || '无法识别包信息，请确认是否由Maven客户端打包，并重新上传'}}</div>
            </div>
            <div v-else>
                <div class="content-file-info" v-if="currentFileName">
                    <span>{{currentFileName}}</span>
                    <bk-icon-plus type="plus-close" @click="() => {
                        currentFileName = ''
                    }" />
                </div>
                <bk-form :label-width="200" :model="formData" form-type="vertical">
                    <bk-form-item label="Group ID" property="groupId">
                        <bk-input readonly v-model="formData.groupId"></bk-input>
                    </bk-form-item>
                    <bk-form-item label="Artifact ID" property="artifactId">
                        <bk-input readonly v-model="formData.artifactId"></bk-input>
                    </bk-form-item>
                    <bk-form-item label="Version" property="version">
                        <bk-input readonly v-model="formData.version"></bk-input>
                    </bk-form-item>
                    <bk-form-item v-if="formData.classifier" label="Classifier" property="classifier">
                        <bk-input readonly v-model="formData.classifier"></bk-input>
                    </bk-form-item>
                    <bk-form-item label="Type" property="type">
                        <bk-input readonly v-model="formData.type"></bk-input>
                    </bk-form-item>
                </bk-form>
            </div>
        </div>
        <div slot="footer">
            <bk-button @click.stop.prevent="cancelUploadArtifact">{{$t('cancel')}}</bk-button>
            <bk-button
                class="ml10"
                theme="primary"
                :disabled="customSettings.uploadFlag || customSettings.saveBtnDisable"
                @click.stop.prevent="submitData">
                {{$t('confirm')}}
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
                customSettings: {
                    isShow: false,
                    title: '确认上传内容',
                    uploadFlag: true, // 是否显示上传组件
                    saveBtnDisable: false // 确认按钮是否禁用
                },
                formData: {
                    uuid: '',
                    groupId: '',
                    artifactId: '',
                    version: '',
                    classifier: '',
                    type: ''
                },
                currentFileName: '', // 当前上传的文件名
                errorMsg: '', // 上传接口后台返回的错误信息
                uploadPercent: 0,
                uploadXhr: null, // 上传请求的xhr对象，用于上传和取消上传时所用
                isLoading: false // 上传选择框是否在加载中状态，因为不能禁用，所以选择使用loading代替

            }
        },
        computed: {
        },
        created () {

        },
        methods: {
            ...mapActions([
                'uploadArtifactory',
                'submitMavenArtifactory',
                'deleteErrorPackage'
            ]),
            handleClickClose () {
                this.customSettings = {
                    isShow: false,
                    title: '确认上传内容',
                    uploadFlag: true, // 是否显示上传组件
                    saveBtnDisable: false // 确认按钮是否禁用
                }
                // 关闭弹窗时重置滚动条需要的数据
                this.onAbortUpload()
                this.$emit('cancel', false)
            },
            onRequestUpload (uploadFile) {
                this.isLoading = true
                this.errorMsg = ''
                this.uploadXhr = new XMLHttpRequest()
                this.uploadArtifactory({
                    xhr: this.uploadXhr,
                    projectId: this.projectId,
                    repoName: this.repoName,
                    body: uploadFile.fileList[0].origin,
                    progressHandler: ($event) => {
                        const num = $event.loaded / $event.total
                        uploadFile.onProgress({ percent: num })
                        this.uploadPercent = num
                        this.currentFileName = uploadFile.fileList[0].origin.name || ''
                    },
                    fullPath: uploadFile.fileList[0].origin.name,
                    headers: {
                        'Content-Type': uploadFile.fileList[0].origin.type || 'application/octet-stream',
                        'X-BKREPO-EXPIRES': 0
                    },
                    uploadType: 'mavenUpload'

                }).then(res => {
                    this.customSettings.uploadFlag = false
                    this.formData = res.data
                }).catch(error => {
                    this.isLoading = false
                    // error &&  this.errorMsg = error.message || error.error || '无法识别包信息，请确认是否由Maven客户端打包，并重新上传'
                    error && (this.errorMsg = error.message || error.error || error)
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
            // 文件解析成功后点击取消按钮
            cancelUploadArtifact () {
                if (this.projectId
                    && this.repoName
                    && this.formData.groupId
                    && this.formData.artifactId
                    && this.formData.version
                    && this.currentFileName
                ) {
                    this.deleteErrorPackage({
                        projectId: this.projectId,
                        repoName: this.repoName,
                        groupId: this.formData.groupId,
                        artifactId: this.formData.artifactId,
                        version: this.formData.version,
                        artifactName: this.currentFileName
                    })
                }
                this.$emit('cancel', false)
            },
            submitData () {
                this.customSettings.saveBtnDisable = true
                const params = {
                    projectId: this.projectId,
                    repoName: this.repoName,
                    body: { ...this.formData }
                }
                this.submitMavenArtifactory(params).then(res => {
                    this.$emit('update', false)
                }).catch(error => {
                    this.$bkMessage({
                        theme: 'error',
                        message: `${error.message || '上传出错了，请重新上传'}`
                    })
                }).finally(() => {
                    this.customSettings.saveBtnDisable = false
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
.content-container{
    width: 100%;
    padding: 20px 30px;
}
.content-info{
    margin: 0 0 9px 0;
    font-style: normal;
    font-weight: 400;
    font-size: 12px;
    line-height: 16px;
    color: #081E40;
}
.content-upload{
    height: 181px;
}
.error-upload-info{
    color: red;
}
.content-file-info{
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
.upload-show-file-container{
    height: 70px;
    padding: 5px;
    display: flex;
    justify-content: space-between;
    width: 100% ;
    &-info{
        text-align: center;
        width: 78%;
    }
    &-cancel{
        margin: 22px 0 0 0;
    }
}
::v-deep .progress-text{
    margin: 0 0 0 10px;
}
::v-deep .bk-upload.draggable .file-wrapper{
    height: 100%;
}
::v-deep .bk-sideslider-footer{
    display: flex;
    align-items: center;
    justify-content: flex-end;
    padding: 0 30px 0 0 ;
}
::v-deep .all-file{
    display: none;
}
</style>

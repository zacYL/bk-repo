<template>
    <canway-dialog
        v-model="previewDialog.show"
        :title="previewDialog.title"
        :width="dialogWidth"
        height-num="705"
        @cancel="previewDialog.show = false">
        <div v-bkloading="{ isLoading }">
            <div class="mb5 preview-file-tips">{{ $t('previewFileTips') }}</div>
            <textarea class="textarea" v-model="basicFileText" readonly></textarea>
        </div>
        <template #footer>
            <bk-button theme="primary" @click="previewDialog.show = false">{{ $t('confirm') }}</bk-button>
        </template>
    </canway-dialog>
</template>

<script>
    import { mapActions } from 'vuex'
    export default {
        name: 'previewBasicFileDialog',
        data () {
            return {
                isLoading: false,
                previewDialog: {
                    show: false,
                    title: '',
                    projectId: '',
                    repoName: '',
                    fullPath: ''
                },
                basicFileText: '',
                dialogWidth: window.innerWidth - 600
            }
        },
        methods: {
            ...mapActions(['previewBasicFile', 'previewCompressedBasicFile']),
            setData (data) {
                const { show, projectId, repoName, fullPath, filePath } = data
                this.previewDialog = {
                    ...this.previewDialog,
                    ...data
                }
                if (!show) return
                this.isLoading = true
                const fn = filePath ? this.previewCompressedBasicFile : this.previewBasicFile
                fn({
                    projectId,
                    repoName,
                    fullPath,
                    filePath
                }).then(res => {
                    this.basicFileText = typeof res === 'string' ? res : JSON.stringify(res)
                }).finally(() => {
                    this.isLoading = false
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
.preview-file-tips {
    color: var(--fontSubsidiaryColor)
}
.textarea {
    resize: none;
    width: 100%;
    height: 500px;
    padding: 10px;
    border: 1px solid var(--borderColor);
}
</style>

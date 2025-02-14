<template>
    <canway-dialog
        :value="show"
        :title="titleMap[type]"
        :height-num="400"
        :width="560"
        @cancel="handleClickCancel"
        v-loading="isLoading"
    >
        <bk-form ref="formRef" :model="form" class="mr10" :label-width="111" :rules="rules">
            <bk-form-item :label="$t('repoType')" property="type" required>
                <bk-select
                    v-model="form.type"
                    :placeholder="$t('artifactTypePlaceholder')"
                    :clearable="false">
                    <bk-option v-for="item in artifactTypeList" :key="item" :id="item" :name="item"></bk-option>
                </bk-select>
            </bk-form-item>
            <bk-form-item :label="$t('artifactName')" property="packageKey" required>
                <bk-input
                    v-model="form.packageKey"
                    :placeholder="$t('artifactNamePlaceholder')"
                    :clearable="false"
                />
            </bk-form-item>
            <bk-form-item :label="$t('searchConditionVersion')" property="versions">
                <bk-input
                    type="textarea"
                    v-model="form.versions"
                />
                <template #tip>
                    <span class="tip">{{$t('proxyWhiteArtifactVersionTips')}}</span>
                </template>
            </bk-form-item>
        </bk-form>
        <template #footer>
            <bk-button theme="default" @click="handleClickCancel">{{$t('cancel')}}</bk-button>
            <bk-button theme="primary" @click="handleClickConfirm">{{$t('confirm')}}</bk-button>
        </template>
    </canway-dialog>
</template>

<script>
    import _ from 'lodash'
    import { mapActions, mapState } from 'vuex'

    export default {
        props: {
            show: {
                type: Boolean,
                required: true
            },
            type: {
                type: String,
                required: true
            },
            artifact: {
                type: Object,
                required: true
            }
        },
        data () {
            return {
                titleMap: {
                    add: this.$t('addArtifactToProxyWhite'),
                    edit: this.$t('editArtifactInfo')
                },
                form: this.getNewForm(),
                isLoading: false
            }
        },
        computed: {
            ...mapState(['artifactTypeList']),
            rules () {
                return {
                    type: [{
                        required: true,
                        message: this.$t('artifactTypePlaceholder'),
                        trigger: 'blur'
                    }],
                    packageKey: [
                        {
                            required: true,
                            message: this.$t('artifactNamePlaceholder'),
                            trigger: 'blur'
                        },
                        ...this.form.type === 'MAVEN'
                            ? [{
                                regex: /[a-zA-Z0-9_\-.]+:[a-zA-Z0-9_\-.]+/,
                                message: this.$t('formatInvalidTip') + 'groupId:artifactId',
                                trigger: 'change'
                            }]
                            : []
                    ]
                }
            }
        },
        watch: {
            type (val) {
                if (!this.show) return
                if (val === 'edit') {
                    this.form = _.cloneDeep(this.artifact)
                    this.form.versions = this.artifact.versions.join('\n')
                } else if (val === 'add') {
                    this.form = this.getNewForm()
                }
            }
        },
        methods: {
            ...mapActions(['addWhiteList', 'editWhiteList']),
            getNewForm () {
                return {
                    type: this.artifactTypeList?.[0] || '',
                    versions: '',
                    packageKey: ''
                }
            },
            // 关闭弹框
            handleClickCancel () {
                this.$emit('close')
            },
            // 提交弹框
            handleClickConfirm () {
                this.$refs.formRef.validate()
                    .then(() => {
                        const form = _.cloneDeep(this.form)
                        form.versions = this.form.versions.split('\n').filter(a => a)
                        const promise = this.type === 'edit' ? this.editWhiteList(form) : this.addWhiteList(form)
                        return promise
                            .then(() => {
                                this.$bkMessage({
                                    theme: 'success',
                                    message: `${this.type === 'add' ? this.$t('add') : this.$t('edit')}` + this.$t('space') + this.$t('success')
                                })
                                this.$emit('close')
                                this.$emit('update')
                            })
                            .catch(err => {
                                this.$bkMessage({
                                    theme: 'error',
                                    message: err.message || this.$t('unknownError')
                                })
                            })
                    })
                    .catch(() => {})
                    .finally(() => {
                        this.isLoading = false
                    })
            }
        }
    }
</script>

<style lang="scss" scoped>
    .tip {
        color: #8797AA;
    }
</style>

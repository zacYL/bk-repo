<template>
    <canway-dialog
        :value="show"
        :title="titleMap[type]"
        :height-num="400"
        :width="520"
        @cancel="handleClickCancel"
        v-loading="isLoading"
    >
        <bk-form ref="formRef" :model="form" class="mr10" :label-width="75" :rules="rules">
            <bk-form-item label="制品类型" property="type" required>
                <bk-select
                    v-model="form.type"
                    placeholder="请选择制品类型"
                    :clearable="false">
                    <bk-option v-for="item in artifactTypeList" :key="item" :id="item" :name="item"></bk-option>
                </bk-select>
            </bk-form-item>
            <bk-form-item label="制品名称" property="packageKey" required>
                <bk-input
                    v-model="form.packageKey"
                    placeholder="请填写制品名称"
                    :clearable="false"
                />
            </bk-form-item>
            <bk-form-item label="版本号" property="versions">
                <bk-input
                    type="textarea"
                    v-model="form.versions"
                />
                <template #tip>
                    <span class="tip">每一行对应一个版本号，请注意书写格式。</span>
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
                    add: '添加制品到白名单',
                    edit: '编辑制品信息'
                },
                form: this.getNewForm(),
                rules: {
                    type: [{
                        required: true,
                        message: '请选择制品类型',
                        trigger: 'blur'
                    }],
                    packageKey: [{
                        required: true,
                        message: '请填写制品名称',
                        trigger: 'blur'
                    }, {
                        regex: /[a-z0-9_\-.]+:[a-z0-9_\-.]+/,
                        message: '非法制品名称',
                        trigger: 'change'
                    }]
                },
                isLoading: false
            }
        },
        computed: {
            ...mapState(['artifactTypeList'])
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
                        form.versions = this.form.versions.split('\n')
                        return this.type === 'edit' ? this.editWhiteList(form) : this.addWhiteList(form)
                    })
                    .then(() => {
                        this.$bkMessage({
                            theme: 'success',
                            message: `${this.type === 'add' ? '添加' : '编辑'}成功`
                        })
                        this.$emit('close')
                        this.$emit('update')
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

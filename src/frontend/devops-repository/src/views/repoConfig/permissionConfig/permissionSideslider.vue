<!--
 * @Date: 2024-11-22 11:03:13
 * @LastEditors: xiaoshan
 * @LastEditTime: 2024-11-22 11:42:18
 * @FilePath: /artifact/src/frontend/devops-repository/src/views/repoConfig/permissionConfig/permissionSideslider.vue
-->
<template>
    <div>
        <bk-sideslider :is-show.sync="visible" :title="title" :quick-close="true" direction="left" :show-mask="false">
            <div slot="content">
                <bk-form ref="permissionForm" :label-width="200">
                    <bk-form-item
                        :label="$t('name')"
                        required
                        :property="'name'"
                        :error-display-type="'normal'">
                        <bk-input v-model="form.name" :placeholder="$t('pleaseInput')"></bk-input>
                    </bk-form-item>
                    <bk-form-item
                        :label="$t('includePath')"
                        required>
                        <bk-button @click="addPath()" class="mr5">{{ $t("addPath") }}</bk-button>
                    </bk-form-item>
                    <bk-form-item
                        v-for="(item, index) in form.path"
                        required
                        :rules="rules.path"
                        :property="'path.' + index + '.value'"
                        :key="index"
                        style="width: fit-content;"
                        class="mt10">
                        <bk-input v-model="item.value" :placeholder="$t('pleaseInput')" style="width: 180px;">
                        </bk-input>
                        <Icon class="hover-btn" size="24" name="icon-delete" @click.native.stop="form.path.splice(index, 1)" style="position: absolute; right: -30px; top: 4px;" />
                    </bk-form-item>
                </bk-form>
            </div>
            <div slot="footer" class="flex-between-center" style="width: 100%;">
                <div></div>
                <bk-button theme="primary" @click="submit()">{{ type === 'create' ? $t('create') : $t('save')}}</bk-button>
            </div>
        </bk-sideslider>
    </div>
</template>

<script>

    import { cloneDeep } from 'lodash'

    const BASE_FORM = {
        name: '',
        path: []
    }

    export default {
        props: {
            isShow: Boolean,
            title: String,
            insertForm: Object,
            type: {
                type: String,
                default: 'create'
            }
        },
        data () {
            return {
                visible: false,
                form: cloneDeep(BASE_FORM),
                rules: {
                    name: [
                        {
                            required: true,
                            message: this.$t('cantSaveEmptyString'),
                            trigger: 'blur'
                        }
                    ],
                    path: [
                        {
                            required: true,
                            message: this.$t('cantSaveEmptyString'),
                            trigger: 'blur'
                        },
                        {
                            validator: this.checkEmptyPath,
                            message: this.$t('cantSaveEmptyString'),
                            trigger: 'blur'
                        },
                        {
                            validator: this.checkSamePath('path'),
                            message: this.$t('cantPassSamePath'),
                            trigger: 'blur'
                        },
                        {
                            max: 32,
                            message: this.$t('maxNumTips', [32]),
                            trigger: 'blur'
                        }
                    ]
                }
            }
        },
        watch: {
            /**
             * @description: 初始化展示状态
             * @return {*}
             */
            isShow: {
                handler (val) {
                    this.visible = val
                    if (val) {
                        this.initPermissionFrom()
                    }
                },
                immediate: true
            },
            /**
             * @description: 同步侧栏展示状态
             * @param {*} val
             * @return {*}
             */
            visible (val) {
                this.$emit('update:isShow', val)
            }
        },
        methods: {
            /**
             * @description: 初始化权限表单
             * @return {*}
             */
            initPermissionFrom () {
                if (this.type === 'create') {
                    this.form = cloneDeep(BASE_FORM)
                } else if (this.type === 'edit') {
                    this.form = cloneDeep(this.insertForm)
                }
            },
            /**
             * @description: 补充路径
             * @return {*}
             */
            addPath () {
                this.form.path.push({
                    value: ''
                })
            },

            submit () {
                const cb = () => {
                    this.visible = false
                }
                this.$refs.permissionForm.validate(valid => {
                    if (valid) {
                        this.$emit('submit', this.form, cb)
                    }
                })
            },

            /**
             * @description: 空校验
             * @param {*} val
             * @return {*}
             */
            checkEmptyPath (val) {
                const v = val || ''
                return !!(v.trim())
            },

            /**
             * @description: 同名路径校验
             * @param {*} key
             * @return {*}
             */
            checkSamePath (key) {
                return (val) => {
                    const num = this.form[key].filter(item => {
                        return item.value === val
                    }).length
                    return !(num > 1)
                }
            }
        }
    }
</script>

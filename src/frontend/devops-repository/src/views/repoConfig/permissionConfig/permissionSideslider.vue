<!--
 * @Date: 2024-11-22 11:03:13
 * @LastEditors: xiaoshan
 * @LastEditTime: 2024-12-04 15:53:29
 * @FilePath: /artifact/src/frontend/devops-repository/src/views/repoConfig/permissionConfig/permissionSideslider.vue
-->
<template>
    <div>
        <bk-sideslider
            direction="right"
            width="600"
            :is-show.sync="visible"
            :title="title"
            :quick-close="true"
            :show-mask="false">
            <div slot="content" class="pt10" style="height: 100%;overflow: auto;">
                <bk-form :model="form" ref="permissionForm" :label-width="100">
                    <bk-form-item
                        :label="$t('name')"
                        required
                        property="name"
                        :rules="rules.name"
                        :error-display-type="'normal'">
                        <bk-input
                            v-model="form.name"
                            :placeholder="$t('pleaseInput')"
                            type="text"
                            :maxlength="32"
                            show-word-limit="true"
                            style="width: 250px;"></bk-input>
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
                        <bk-input
                            v-model="item.value"
                            :maxlength="255"
                            show-word-limit="true"
                            :placeholder="$t('pleaseInput')" style="width: 250px;">
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
                        },
                        {
                            max: 32,
                            message: this.$t('maxNumTips', [32]),
                            trigger: 'blur'
                        },
                        {
                            validator: function (val) {
                                if (!val) return true
                                return val.match(/^[a-zA-Z0-9\u4e00-\u9fa5_-]+$/)
                            },
                            message: this.$t('pathNameCheckTips'),
                            trigger: 'blur'
                        }
                    ],
                    path: [
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
                            regex: /^(\/[^\\:*?"<>|\s]{0,254})+$/,
                            message: this.$t('folderPathPlaceholder2'),
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
                    const { name, path } = cloneDeep(this.insertForm)
                    this.form.name = name
                    this.form.path = path.map(item => ({ value: item }))
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
                if (this.form.path.length === 0) {
                    this.$bkMessage({
                        theme: 'error',
                        message: this.$t('pleaseAddPath')
                    })
                    return
                }
                this.$refs.permissionForm.validate(valid => {
                    if (valid) {
                        this.$emit('submit', this.form, cb)
                    }
                }).catch(() => {
                    
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
                        // 路径比较，默认最后一个加上‘/’
                        const cb = (value) => {
                            if (!value.endsWith('/')) {
                                value += '/'
                            }
                            return value
                        }
                        const target = cb(item.value)
                        const current = cb(val)
                        return target === current
                    }).length
                    return !(num > 1)
                }
            }
        }
    }
</script>

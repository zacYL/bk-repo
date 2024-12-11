<!--
 * @Date: 2024-11-21 14:16:15
 * @LastEditors: xiaoshan
 * @LastEditTime: 2024-12-11 11:07:54
 * @FilePath: /artifact/src/frontend/devops-repository/src/views/repoScan/scanMain/components/components/AddBlackWhiteRepoDialog.vue
-->
<template>
    <bk-dialog
        v-model="visible"
        :title="title"
        header-position="left"
        width="380"
        ext-cls="add-black-white-repo-dialog"
        @cancel="cancel"
    >
        <bk-form
            class="mr10"
            :label-width="135"
            form-type="vertical"
            :model="form"
            :rules="rules"
            ref="Form"
        >
            <!-- 制品类型 -->
            <bk-form-item :label="$t('repoType')" required property="repoType">
                <bk-select
                    v-model="form.repoType"
                    :placeholder="$t('allTypes')"
                >
                    <bk-option
                        v-for="type in repoEnum.filter(v => v.value !== 'generic')"
                        :key="type.value"
                        :id="type.value"
                        :name="type.label"
                    >
                        <div class="flex-align-center">
                            <Icon size="20" :name="type.value" />
                            <span class="ml10 flex-1 text-overflow">{{ type.label }}</span>
                        </div>
                    </bk-option>
                </bk-select>
            </bk-form-item>

            <!-- groupID -->
            <bk-form-item
                v-if="['maven', 'gradle'].includes(form.repoType)"
                label="groupID"
                required
                property="groupID"
                error-display-type="normal"
            >
                <bk-input
                    type="text"
                    :placeholder="$t('pleaseInput') + $t('space') + 'groupID'"
                    v-model.trim="form.groupID"
                    maxlength="32"
                    show-word-limit
                ></bk-input>
            </bk-form-item>

            <!-- 制品名 -->
            <bk-form-item
                :label="$t('artifactName')"
                required
                property="name"
                error-display-type="normal"
            >
                <bk-input
                    type="text"
                    :placeholder="['go', 'conan'].includes(form.repoType) ? $t('modulePathTips') : $t('artifactNamePlaceholder')"
                    v-model.trim="form.name"
                    maxlength="128"
                    show-word-limit
                ></bk-input>
            </bk-form-item>

            <!-- 版本逻辑 -->
            <bk-form-item
                :label="$t('versionOperators')"
                required
                property="operator"
            >
                <bk-select
                    v-model="form.operator"
                    :placeholder="$t('pleaseSelect') + $t('space') + $t('versionOperators')"
                >
                    <bk-option
                        v-for="item in (form.repoType === 'docker' ? DockerVersionComparisonOperator : VersionComparisonOperator)"
                        :key="item"
                        :id="item"
                        :name="item"
                    >
                        <div class="flex-align-center">
                            <span class="ml10 flex-1 text-overflow">{{ item }}</span>
                        </div>
                    </bk-option>
                </bk-select>
            </bk-form-item>

            <!-- 版本 -->
            <bk-form-item
                :label="$t('version')"
                required
                property="version"
                error-display-type="normal"
            >
                <bk-input
                    type="text"
                    :placeholder="$t('pleaseInput') + $t('space') + $t('version')"
                    v-model.trim="form.version"
                    maxlength="64"
                    show-word-limit
                ></bk-input>
            </bk-form-item>

            <!-- 过期时间 -->
            <bk-form-item
                v-if="isWhite"
                :label="$t('expire')"
                property="expireDate"
                error-display-type="normal"
            >
                <bk-date-picker
                    style="width: 100%;"
                    v-model="form.expireDate"
                    format="yyyy-MM-dd 23:59:59"
                    :placeholder="$t('pleaseSelect') + $t('expire')"
                    :options="{ disabledDate: function (time) {
                        return time.getTime() <= Date.now() - 86400000
                    } }"
                />
            </bk-form-item>

        </bk-form>

        <template #footer>
            <bk-button theme="default" @click="cancel">{{ $t('cancel') }}</bk-button>
            <bk-button
                class="ml10"
                :loading="btnLoading"
                theme="primary"
                @click="submit"
            >{{ $t('confirm') }}</bk-button>
        </template>
    </bk-dialog>
</template>

<script>
    import { repoEnum, VersionComparisonOperator, DockerVersionComparisonOperator, OperatorMap } from '@repository/store/publicEnum'
    import { cloneDeep } from 'lodash'
    const baseForm = {
        repoType: 'maven',
        name: '*',
        operator: '',
        version: '',
        groupID: '',
        expireDate: ''
    }
    export default {
        name: 'createScan',
        props: {
            visible: Boolean,
            title: String,
            isWhite: Boolean
        },
        data () {
            return {
                repoEnum,
                VersionComparisonOperator,
                DockerVersionComparisonOperator,
                form: cloneDeep(baseForm),
                btnLoading: false
            }
        },
        computed: {
            rules () {
                return {
                    name: [
                        {
                            required: true,
                            message: this.$t('artifactNamePlaceholder'),
                            trigger: 'blur'
                        },
                        ...['maven', 'gradle'].includes(this.form.repoType)
                            ? [
                                {
                                    regex: /^[a-zA-Z0-9._-]*$|^[*]$/,
                                    message: this.$t('BlackWhiteAddCheckTips2'),
                                    trigger: 'blur'
                                }
                            ]
                            : [
                                {
                                    regex: /^[a-zA-Z0-9._-]+$/,
                                    message: this.$t('BlackWhiteAddCheckTips'),
                                    trigger: 'blur'
                                }
                            ]
                    ],
                    repoType: [
                        {
                            required: true,
                            message: this.$t('artifactTypePlaceholder'),
                            trigger: 'blur'
                        }
                    ],
                    operator: [
                        {
                            required: true,
                            message: this.$t('pleaseSelect') + this.$t('space') + this.$t('versionOperators'),
                            trigger: 'blur'
                        }
                    ],
                    groupID: [
                        {
                            required: true,
                            message: this.$t('pleaseInput') + this.$t('space') + 'groupID',
                            trigger: 'blur'
                        }
                    ],
                    // docker版本实际上是tab，不遵循语义化规则，所以前端不限制
                    ...this.form.repoType !== 'docker'
                        ? {
                            version: [
                                {
                                    required: true,
                                    message: this.$t('pleaseInput') + this.$t('space') + this.$t('version'),
                                    trigger: 'blur'
                                },
                                {
                                    regex: /^[a-zA-Z0-9._-]+$/,
                                    message: this.$t('BlackWhiteAddCheckTips'),
                                    trigger: 'blur'
                                }
                            ]
                        }
                        : {}
                }
            }
        },
        watch: {
            'form.repoType': {
                handler (val) {
                    // maven/gradle 需要设置groupID
                    // 每次切换默认改名字，'maven', 'gradle' 默认*，其他默认‘’
                    if (['maven', 'gradle'].includes(val)) {
                        this.form.name = '*'
                    } else {
                        this.form.name = ''
                    }
                    // 每次切换制品类型之后都要重置操作类型
                    this.form.operator = ''
                }
            },
            visible (val) {
                if (val) {
                    this.$refs.Form.clearError()
                    this.form = cloneDeep(baseForm)
                }
            }
        },
        methods: {
            cancel () {
                this.$emit('cancel')
            },
            submit () {
                this.btnLoading = true
                const cb = () => {
                    this.btnLoading = false
                }
                this.$refs.Form.validate().then(() => {
                    const subForm = cloneDeep(this.form)
                    // 处理 expireDate
                    if (subForm.expireDate) {
                        const date = new Date(subForm.expireDate)
                        date.setHours(23 + 8, 59, 59)
                        const lastDotIndex = date.toISOString().lastIndexOf('.')
                        subForm.expireDate = date.toISOString().slice(0, lastDotIndex)
                    }
                    Object.keys(OperatorMap).forEach(key => {
                        if (OperatorMap[key] === subForm.operator) {
                            subForm.operator = key
                        }
                    })
                    this.$emit('submit', subForm, cb)
                }).catch(() => {
                    cb()
                })
            }
        }
    }
</script>
<style lang="scss">
.add-black-white-repo-dialog {
    .bk-dialog-body {
        min-height: unset !important;
    }
}
</style>

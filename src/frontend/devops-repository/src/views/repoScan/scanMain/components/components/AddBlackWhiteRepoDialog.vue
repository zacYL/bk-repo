<!--
 * @Date: 2024-11-21 14:16:15
 * @LastEditors: xiaoshan
 * @LastEditTime: 2024-12-03 14:22:33
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
            <bk-form-item :label="$t('repoType')" :required="true" property="repoType">
                <bk-select
                    v-model="form.repoType"
                    :placeholder="$t('allTypes')"
                >
                    <bk-option
                        v-for="type in repoEnum"
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
                v-if="('maven', 'gradle').includes(form.repoType)"
                label="groupID"
                :required="true"
                property="groupID"
                error-display-type="normal"
            >
                <bk-input
                    type="text"
                    :placeholder="$t('pleaseSelect') + $t('space') + 'groupID'"
                    v-model.trim="form.groupID"
                    maxlength="32"
                    show-word-limit
                ></bk-input>
            </bk-form-item>

            <!-- 制品名 -->
            <bk-form-item
                :label="$t('artifactName')"
                :required="true"
                property="name"
                error-display-type="normal"
            >
                <bk-input
                    type="text"
                    :placeholder="$t('artifactNamePlaceholder')"
                    v-model.trim="form.name"
                    maxlength="128"
                    show-word-limit
                ></bk-input>
            </bk-form-item>

            <!-- 版本逻辑 -->
            <bk-form-item
                :label="$t('versionOperators')"
                :required="true"
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
                :required="true"
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
    import { repoEnum, VersionComparisonOperator, DockerVersionComparisonOperator } from '@repository/store/publicEnum'
    import { cloneDeep } from 'lodash'
    const baseForm = {
        repoType: 'maven',
        name: '',
        operator: '==',
        version: '',
        groupID: ''
    }
    export default {
        name: 'createScan',
        props: {
            visible: Boolean,
            title: String
        },
        data () {
            return {
                repoEnum,
                VersionComparisonOperator,
                DockerVersionComparisonOperator,
                form: cloneDeep(baseForm),
                rules: {
                    name: [
                        {
                            required: true,
                            message: this.$t('artifactNamePlaceholder'),
                            trigger: 'blur'
                        },
                        {
                            regex: /^[a-zA-Z0-9._-]+$/,
                            message: this.$t('BlackWhiteAddCheckTips'),
                            trigger: 'blur'
                        }
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

                },
                btnLoading: false
            }
        },
        watch: {
            'form.repoType': {
                handler (val) {
                    // maven/gradle 需要设置groupID
                    if (['maven', 'gradle'].includes(val)) {
                        this.form.groupID = '*'
                    }
                    // 每次切换制品类型之后都要重置操作类型
                    this.form.operator = ''
                }
            },
            visible (val) {
                if (val) {
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
                    this.$emit('submit', this.form, cb)
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

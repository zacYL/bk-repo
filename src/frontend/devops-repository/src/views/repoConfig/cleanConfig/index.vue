<template>
    <bk-form class="clean-config-container" :label-width="120" :model="config" :rules="rules" ref="cleanForm">
        <bk-form-item label="自动清理">
            <bk-switcher v-model="config.autoClean" size="small" theme="primary" @change="clearError"></bk-switcher>
            <div class="form-tip">制品版本数量超过保留版本数，且在保留时间内没有使用过的制品版本将会被清理</div>
        </bk-form-item>
        <bk-form-item label="最少保留版本" required property="reserveVersions" error-display-type="normal">
            <bk-input class="w250" v-model="config.reserveVersions" :disabled="!config.autoClean"></bk-input>
        </bk-form-item>
        <bk-form-item label="最少保留时间(天)" required property="reserveDays" error-display-type="normal">
            <bk-input class="w250" v-model="config.reserveDays" :disabled="!config.autoClean"></bk-input>
        </bk-form-item>
        <bk-form-item label="保留规则">
            <div class="rule-add flex-center" :class="{ 'disabled': !config.autoClean }" @click="addRule()">
                <i class="mr5 devops-icon icon-plus-circle"></i>
                添加规则
            </div>
            <div class="form-tip">满足保留规则的制品将不会被清理</div>
            <div class="rule-list">
                <package-clean-rule
                    class="mt10"
                    v-for="(rule, ind) in config.rules"
                    :key="ind"
                    :disabled="!config.autoClean"
                    v-bind="rule"
                    @change="r => config.rules.splice(ind, 1, r)"
                    @delete="config.rules.splice(ind, 1)">
                </package-clean-rule>
            </div>
        </bk-form-item>
        <bk-form-item>
            <bk-button theme="primary" @click="save()">{{$t('save')}}</bk-button>
        </bk-form-item>
    </bk-form>
</template>
<script>
    import packageCleanRule from './packageCleanRule'
    import { mapActions } from 'vuex'
    export default {
        name: 'cleanConfig',
        components: {
            packageCleanRule
        },
        props: {
            baseData: Object
        },
        data () {
            return {
                loading: false,
                config: {
                    autoClean: false,
                    reserveVersions: 20,
                    reserveDays: 30,
                    rules: []
                },
                rules: {
                    reserveVersions: [
                        {
                            regex: /^[0-9]+$/,
                            message: '请填写非负整数',
                            trigger: 'blur'
                        }
                    ],
                    reserveDays: [
                        {
                            regex: /^[0-9]+$/,
                            message: '请填写非负整数',
                            trigger: 'blur'
                        }
                    ]
                }
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            repoName () {
                return this.$route.query.repoName
            }
        },
        watch: {
            baseData: {
                handler (val) {
                    if (!val.configuration.cleanStrategy) return
                    const {
                        autoClean = false,
                        reserveVersions = 20,
                        reserveDays = 30,
                        rule = { rules: [] }
                    } = val.configuration.cleanStrategy
                    this.config = { ...this.config, autoClean, reserveVersions, reserveDays }
                    this.config.rules = rule.rules.map(r => {
                        return r.rules?.reduce((target, item) => {
                            target[item.field] = {
                                ...item,
                                value: item.operation === 'MATCH' ? item.value.replace(/^\*(.*)\*$/, '$1') : item.value
                            }
                            return target
                        }, {})
                    })
                },
                deep: true,
                immediate: true
            }
        },
        methods: {
            ...mapActions(['updateRepoInfo']),
            addRule () {
                if (!this.config.autoClean) return
                this.config.rules.push({})
            },
            clearError () {
                this.$refs.cleanForm.clearError()
            },
            async save () {
                await this.$refs.cleanForm.validate()
                const { autoClean, reserveVersions, reserveDays } = this.config
                let rules = this.config.rules
                rules = rules.map(rs => {
                    return {
                        relation: 'AND',
                        rules: Object.values(rs).map(i => {
                            return i.value && {
                                ...i,
                                value: i.operation === 'MATCH' ? `*${i.value}*` : i.value
                            }
                        }).filter(Boolean)
                    }
                })
                this.loading = true
                this.updateRepoInfo({
                    projectId: this.projectId,
                    name: this.repoName,
                    body: {
                        configuration: {
                            ...this.baseData.configuration,
                            cleanStrategy: {
                                autoClean,
                                reserveVersions,
                                reserveDays,
                                rule: {
                                    relation: 'OR',
                                    rules
                                }
                            }
                        }
                    }
                }).then(() => {
                    this.$emit('refresh')
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('save') + this.$t('success')
                    })
                }).finally(() => {
                    this.loading = false
                })
            }
        }
    }
</script>
<style lang="scss" scoped>
.clean-config-container {
    max-width: 1080px;
    .rule-add {
        width: 120px;
        height: 32px;
        color: var(--primaryColor);
        background-color: var(--bgHoverColor);
        cursor: pointer;
        &.disabled {
            color: white;
            background-color: var(--borderWeightColor);
            cursor: not-allowed;
        }
    }
}
</style>

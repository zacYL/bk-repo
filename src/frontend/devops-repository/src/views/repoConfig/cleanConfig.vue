<template>
    <bk-form class="clean-config-container" :label-width="150" :model="config" :rules="rules" ref="cleanForm">
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
                <metadata-rule
                    class="mt10"
                    v-for="(rule, ind) in config.metadataReserveRules"
                    :key="ind"
                    :disabled="!config.autoClean"
                    :rule="rule"
                    @change="r => config.metadataReserveRules.splice(ind, 1, r)"
                    @delete="config.metadataReserveRules.splice(ind, 1)">
                </metadata-rule>
            </div>
        </bk-form-item>
        <bk-form-item>
            <bk-button theme="primary" @click="save()">{{$t('save')}}</bk-button>
        </bk-form-item>
    </bk-form>
</template>
<script>
    import metadataRule from './metadataRule'
    import { mapActions } from 'vuex'
    export default {
        name: 'cleanConfig',
        components: { metadataRule },
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
                    metadataReserveRules: []
                },
                rules: {
                    reserveVersions: [
                        {
                            validator: v => /^[0-9]+$/.test(v),
                            message: '请填写非负整数',
                            trigger: 'blur'
                        }
                    ],
                    reserveDays: [
                        {
                            validator: v => /^[0-9]+$/.test(v),
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
            baseData () {
                this.config = {
                    ...this.config,
                    ...this.baseData.configuration.cleanStrategy
                }
                this.clearError()
            }
        },
        methods: {
            ...mapActions(['updateRepoInfo']),
            addRule () {
                if (!this.config.autoClean) return
                this.config.metadataReserveRules.push({
                    metadataName: '',
                    metadataValue: '',
                    operationType: 'EQ'
                })
            },
            clearError () {
                this.$refs.cleanForm.clearError()
            },
            async save () {
                await this.$refs.cleanForm.validate()
                this.config.metadataReserveRules = this.config.metadataReserveRules.filter(v => {
                    return v.metadataName && v.metadataValue && v.operationType
                })
                this.loading = true
                this.updateRepoInfo({
                    projectId: this.projectId,
                    name: this.repoName,
                    body: {
                        configuration: {
                            ...this.baseData.configuration,
                            cleanStrategy: this.config
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

<template>
    <bk-form style="max-width: 1080px;" :label-width="120" :model="rule" :rules="rules" ref="ruleForm">
        <bk-form-item label="质量规则">
            <bk-switcher v-model="editable" size="small" theme="primary" @change="$refs.ruleForm.clearError()"></bk-switcher>
        </bk-form-item>
        <template>
            <bk-form-item v-if="ruleTypes.includes(SCAN_TYPE_LICENSE)" label="许可证规则">
                <div style="color:var(--fontSubsidiaryColor);">当许可证中出现不符合以下规则的许可证时，则不通过质量规则</div>
                <div class="mt10"><bk-checkbox :disabled="!editable" v-model="rule.recommend">仅有可用许可证</bk-checkbox></div>
                <div class="mt10"><bk-checkbox :disabled="!editable" v-model="rule.compliance">仅有合规的许可证</bk-checkbox></div>
                <div class="mt10"><bk-checkbox :disabled="!editable" v-model="rule.unknown">无未知许可证</bk-checkbox></div>
            </bk-form-item>
            <bk-form-item v-if="ruleTypes.includes(SCAN_TYPE_SECURITY)" label="安全规则">
                <div style="color:var(--fontSubsidiaryColor);">当扫描的制品漏洞超过下方任意一条规则中设定的数量，则制品未通过质量规则</div>
            </bk-form-item>
            <template v-if="ruleTypes.includes(SCAN_TYPE_SECURITY)">
                <bk-form-item label="" v-for="[id, name] in Object.entries(leakLevelEnum)" :key="id"
                    :property="id.toLowerCase()" error-display-type="normal">
                    <div class="flex-align-center">
                        <div :class="`status-sign ${id}`" :data-name="`${name}漏洞≦`"></div>
                        <bk-input class="ml10 mr10" style="width: 80px;"
                            :disabled="!editable" v-model.trim="rule[id.toLowerCase()]"
                            @focus="$refs.ruleForm.clearError()"
                            @blur="$refs.ruleForm.validate()">
                        </bk-input>
                        <span>个</span>
                    </div>
                </bk-form-item>
            </template>
        </template>
        <bk-form-item label="触发事件">
            <div style="color:var(--fontSubsidiaryColor);">可勾选下方按钮，在扫描或扫描结束后触发勾选项</div>
            <!-- <div class="mt10"><bk-checkbox v-model="rule.forbidScanUnFinished">自动禁止使用制品：制品扫描未结束的制品</bk-checkbox></div> -->
            <div class="mt10"><bk-checkbox :disabled="!editable" v-model="rule.forbidQualityUnPass">自动禁止使用制品：质量规则未通过的制品</bk-checkbox></div>
        </bk-form-item>
        <bk-form-item>
            <bk-button theme="primary" @click="save()">{{$t('save')}}</bk-button>
        </bk-form-item>
    </bk-form>
</template>
<script>
    import { mapActions } from 'vuex'
    import { leakLevelEnum } from '@repository/store/publicEnum'
    import { SCAN_TYPE_LICENSE, SCAN_TYPE_SECURITY } from '../../../store/publicEnum'
    export default {
        name: 'scanQualityRule',
        props: {
            projectId: String,
            planId: String,
            scanTypes: Array
        },
        data () {
            const validate = {
                regex: /^[0-9]*$/,
                message: '输入格式错误，请输入非负整数',
                trigger: 'blur'
            }
            return {
                SCAN_TYPE_SECURITY: SCAN_TYPE_SECURITY,
                SCAN_TYPE_LICENSE: SCAN_TYPE_LICENSE,
                leakLevelEnum,
                editable: false,
                rule: {
                    recommend: false,
                    compliance: false,
                    unknown: false,
                    critical: '',
                    high: '',
                    medium: '',
                    low: '',
                    white: '',
                    forbidScanUnFinished: false,
                    forbidQualityUnPass: false
                },
                rules: {
                    critical: [validate],
                    high: [validate],
                    medium: [validate],
                    low: [validate],
                    white: [
                        validate,
                        {
                            validator: () => this.editable ? this.computedEditable() : true,
                            message: '请填写至少一条质量规则',
                            trigger: 'blur'
                        }
                    ],
                    recommend: [
                        {
                            validator: () => this.editable ? this.computedEditable() : true,
                            message: '请填写至少一条质量规则',
                            trigger: 'blur'
                        }
                    ]
                }
            }
        },
        computed: {
            ruleTypes () {
                return this.scanTypes.filter(scanType => scanType === SCAN_TYPE_SECURITY || scanType === SCAN_TYPE_LICENSE)
            }
        },
        created () {
            this.initData()
            this.getRules()
        },
        methods: {
            ...mapActions(['getQualityRule', 'saveQualityRule']),
            initData () {
                this.rule = this.scanTypes.includes('LICENSE')
                    ? {
                        recommend: false,
                        compliance: false,
                        unknown: false,
                        forbidScanUnFinished: false,
                        forbidQualityUnPass: false
                    }
                    : {
                        critical: '',
                        high: '',
                        medium: '',
                        low: '',
                        white: '',
                        forbidScanUnFinished: false,
                        forbidQualityUnPass: false
                    }
            },
            async save () {
                await this.$refs.ruleForm.validate()
                Promise
                    .all(this.ruleTypes.map(type => this.doSave(type)))
                    .then(() => {
                        this.$bkMessage({
                            theme: 'success',
                            message: this.$t('save') + this.$t('success')
                        })
                        this.getRules()
                    })
            },
            doSave (ruleType) {
                return this.saveQualityRule({
                    type: ruleType,
                    id: this.planId,
                    body: Object.keys(this.rule).reduce((target, key) => {
                        const value = this.rule[key]
                        if (typeof value === 'string' && value.length > 0) {
                            target[key] = Number(value)
                        }
                        if (typeof value === 'boolean' || typeof value === 'number') {
                            target[key] = value
                        }
                        return target
                    }, {})
                })
            },
            getRules () {
                Promise.all(
                    this.ruleTypes.map(type => this.getQualityRule({ type: type, id: this.planId }))
                ).then(qualityRules => {
                    qualityRules.forEach(qualityRule => {
                        Object.keys(qualityRule).forEach(k => {
                            qualityRule[k] !== null && (this.rule[k] = qualityRule[k])
                        })
                    })
                    this.editable = this.computedEditable()
                })
            },
            computedEditable () {
                const { critical, high, medium, low, white, recommend, compliance, unknown } = this.rule
                return this.scanTypes.includes('LICENSE')
                    ? Boolean(
                        recommend
                            || compliance
                            || unknown
                    )
                    : Boolean(
                        critical !== ''
                            || high !== ''
                            || medium !== ''
                            || low !== ''
                            || white !== ''
                    )
            }
        }
    }
</script>

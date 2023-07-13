<template>
    <main class="create-node-container" v-bkloading="{ isLoading }">
        <bk-form class="mb20 plan-form" :label-width="100" :model="planForm" :rules="rules" ref="planForm">
            <bk-form-item label="计划名称" :required="true" property="name" error-display-type="normal">
                <bk-input class="w480" v-model.trim="planForm.name" maxlength="32" show-word-limit :disabled="disabled"></bk-input>
            </bk-form-item>
            <bk-form-item label="同步策略"
                :property="{ 'SPECIFIED_TIME': 'time', 'CRON_EXPRESSION': 'cron' }[planForm.executionStrategy]"
                error-display-type="normal">
                <bk-radio-group
                    class="radio-flex"
                    v-model="planForm.executionStrategy"
                    @change="clearError">
                    <bk-radio value="IMMEDIATELY" :disabled="isDisabledExecutionStrategy || disabled">
                        <span class="label-span">立即执行</span>
                    </bk-radio>
                    <bk-radio value="SPECIFIED_TIME" :disabled="isDisabledExecutionStrategy || disabled">
                        <div class="flex-align-center">
                            <span class="label-span">指定时间</span>
                            <bk-date-picker
                                style="width: 180px;"
                                class="ml10"
                                v-if="planForm.executionStrategy === 'SPECIFIED_TIME'"
                                v-model="planForm.time"
                                type="datetime"
                                :disabled="disabled"
                                :options="{
                                    disabledDate: (date) => date < new Date()
                                }">
                            </bk-date-picker>
                        </div>
                    </bk-radio>
                    <bk-radio value="CRON_EXPRESSION" :disabled="isDisabledExecutionStrategy || disabled">
                        <div class="flex-align-center">
                            <span class="label-span">定时执行</span>
                            <template v-if="planForm.executionStrategy === 'CRON_EXPRESSION'">
                                <bk-input v-if="disabled" class="ml10 w180" :value="planForm.cron" :disabled="disabled"></bk-input>
                                <Cron v-else class="ml10" v-model="planForm.cron" />
                            </template>
                        </div>
                    </bk-radio>
                    <bk-radio v-if="planForm.replicaObjectType === 'REPOSITORY'" value="REAL_TIME" :disabled="isDisabledRealTime || disabled">
                        <span class="label-span">实时同步</span>
                    </bk-radio>
                </bk-radio-group>
            </bk-form-item>
            <bk-form-item label="冲突策略" property="conflictStrategy">
                <bk-radio-group v-model="planForm.conflictStrategy">
                    <bk-radio v-for="strategy in conflictStrategyList" :key="strategy.value"
                        :value="strategy.value" :disabled="disabled">
                        <span v-bk-tooltips="{ content: strategy.tip, placements: ['top'] }">{{ strategy.label }}</span>
                    </bk-radio>
                </bk-radio-group>
            </bk-form-item>
            <bk-form-item label="同步类型">
                <card-radio-group
                    v-model="planForm.replicaObjectType"
                    :disabled="disabled"
                    :list="replicaObjectTypeList"
                    @change="changeReplicaObjectType">
                </card-radio-group>
            </bk-form-item>
            <bk-form-item label="同步对象" :required="true" property="config" error-display-type="normal">
                <template v-if="planForm.replicaObjectType === 'REPOSITORY'">
                    <repository-table
                        ref="planConfig"
                        :init-data="replicaTaskObjects"
                        :disabled="disabled"
                        @clearError="clearError">
                    </repository-table>
                </template>
                <template v-else-if="planForm.replicaObjectType === 'PACKAGE'">
                    <package-table
                        ref="planConfig"
                        :init-data="replicaTaskObjects"
                        :disabled="disabled"
                        @clearError="clearError">
                    </package-table>
                </template>
                <template v-else-if="planForm.replicaObjectType === 'PATH'">
                    <path-table
                        ref="planConfig"
                        :init-data="replicaTaskObjects"
                        :disabled="disabled"
                        @clearError="clearError">
                    </path-table>
                </template>
            </bk-form-item>
            <bk-form-item label="目标节点" :required="true" property="remoteClusterIds" error-display-type="normal">
                <bk-select
                    class="w480"
                    v-model="planForm.remoteClusterIds"
                    searchable
                    multiple
                    display-tag
                    :collapse-tag="false"
                    :disabled="disabled">
                    <bk-option v-for="option in clusterList.filter(v => v.type !== 'CENTER')"
                        :key="option.name"
                        :id="option.id"
                        :name="option.name">
                    </bk-option>
                </bk-select>
            </bk-form-item>
            <bk-form-item label="创建者" v-if="routeName !== 'createPlan'">
                {{ userList[planForm.createdBy] && userList[planForm.createdBy].name || planForm.createdBy }}
            </bk-form-item>
            <bk-form-item label="创建时间" v-if="routeName !== 'createPlan'">{{ formatDate(planForm.createdDate) }}</bk-form-item>
            <bk-form-item :label="$t('description')">
                <bk-input
                    class="w480"
                    v-model.trim="planForm.description"
                    type="textarea"
                    :rows="6"
                    maxlength="200"
                    :disabled="disabled">
                </bk-input>
            </bk-form-item>
            <template v-if="planForm.replicaObjectType === 'REPOSITORY'">
                <bk-form-item>
                    <bk-checkbox
                        v-model="noRecordsCheck"
                        :disabled="disabled">
                        {{$t('noDistributionRecord')}}
                    </bk-checkbox>
                </bk-form-item>
                <bk-form-item>
                    <span>{{$t('planLogReserve')}}</span>
                    <bk-input
                        class="w180"
                        :class="{ 'bk-form-item is-error': !Number(recordReserveDays) && errorRecordReserveDaysInfo }"
                        type="number"
                        :max="60"
                        :min="1"
                        v-model="recordReserveDays"
                        :disabled="disabled"
                        :placeholder="$t('planRecordReserveDaysInfo')"
                        @blur="onBlurRecordReserveDays">
                    </bk-input>
                    <p class="form-error-tip" v-if="!Number(recordReserveDays) && errorRecordReserveDaysInfo">{{$t('planRecordReserveDaysInfo')}}</p>
                </bk-form-item>
            </template>
            <bk-form-item v-if="!disabled">
                <bk-button @click="$emit('close')">{{$t('cancel')}}</bk-button>
                <bk-button class="ml10" theme="primary" :loading="planForm.loading" @click="save">{{$t('confirm')}}</bk-button>
            </bk-form-item>
        </bk-form>
    </main>
</template>
<script>
    import { mapState, mapActions } from 'vuex'
    import Cron from '@repository/components/Cron'
    import CardRadioGroup from '@repository/components/CardRadioGroup'
    import repositoryTable from './repositoryTable'
    import packageTable from './packageTable'
    import pathTable from './pathTable'
    import { formatDate } from '@repository/utils'

    export default {
        name: 'createPlan',
        components: { Cron, CardRadioGroup, repositoryTable, packageTable, pathTable },
        props: {
            rowsData: {
                type: Object,
                default: () => {}
            }
        },
        data () {
            return {
                isDisabledRealTime: false,
                conflictStrategyList: [
                    { value: 'SKIP', label: '跳过冲突', tip: '当目标节点存在相同制品时，跳过该制品同步，同步剩余制品' },
                    { value: 'OVERWRITE', label: '替换制品', tip: '当目标节点存在相同制品时，覆盖原制品并继续执行计划' },
                    { value: 'FAST_FAIL', label: '终止同步', tip: '当目标节点存在相同制品时，终止执行计划' }
                ],
                replicaObjectTypeList: [
                    { value: 'REPOSITORY', label: '仓库', tip: '同步多个仓库' },
                    { value: 'PACKAGE', label: '制品', tip: '同步同一仓库下多个制品' },
                    { value: 'PATH', label: '文件', tip: '同步同一仓库下多个文件' }
                ],
                isLoading: false,
                planForm: {
                    loading: false,
                    name: '',
                    executionStrategy: 'IMMEDIATELY',
                    replicaObjectType: 'REPOSITORY',
                    time: new Date(new Date().getTime() + 30 * 60 * 1000),
                    cron: '* * * * * ? *',
                    conflictStrategy: 'SKIP',
                    remoteClusterIds: [],
                    createdBy: '',
                    createdDate: '',
                    description: ''
                },
                rules: {
                    name: [
                        {
                            required: true,
                            message: this.$t('pleaseInput') + '计划名称',
                            trigger: 'blur'
                        }
                    ],
                    time: [
                        {
                            required: true,
                            message: this.$t('pleaseSelect') + '时间',
                            trigger: 'blur'
                        },
                        {
                            validator: date => date > new Date(),
                            message: '当前时间已过期',
                            trigger: 'blur'
                        }
                    ],
                    cron: [
                        {
                            required: true,
                            message: this.$t('pleaseInput') + 'cron表达式',
                            trigger: 'blur'
                        }
                    ],
                    config: [
                        {
                            validator: () => {
                                return this.$refs.planConfig.getConfig()
                            },
                            message: '请完善同步对象信息',
                            trigger: 'blur'
                        }
                    ],
                    remoteClusterIds: [
                        {
                            required: true,
                            message: this.$t('pleaseSelect') + '目标节点',
                            trigger: 'blur'
                        }
                    ]
                },
                replicaTaskObjects: [],
                noRecordsCheck: true,
                recordReserveDays: 30,
                errorRecordReserveDaysInfo: false
            }
        },
        computed: {
            ...mapState(['clusterList', 'userList']),
            projectId () {
                return this.$route.params.projectId
            },
            routeName () {
                return this.rowsData.routeName
            },
            disabled () {
                return this.routeName === 'planDetail'
            },
            /**
             * 设置同步策略单选框
             * 在分发计划编辑的时候，如果当前的策略是 [立即执行/指定时间/定时执行]，就不允许更改为[实时同步]。
             * 如果当前的策略是[实时同步]，就不允许更改
             * */
            isDisabledExecutionStrategy () {
                let isDisabled = false

                if (this.routeName === 'editPlan') {
                    const { executionStrategy } = this.planForm
                    const executionStatus = ['IMMEDIATELY', 'SPECIFIED_TIME', 'CRON_EXPRESSION'].includes(executionStrategy)

                    if (executionStrategy === 'REAL_TIME' && executionStatus === false) {
                        isDisabled = true
                        this.isDisabledRealTime = false
                    } else {
                        isDisabled = false
                        this.isDisabledRealTime = true
                    }
                }

                return isDisabled
            }
        },
        created () {
            this.getRepoListAll({ projectId: this.rowsData.projectId })
            this.routeName !== 'createPlan' && this.handlePlanDetail()
        },
        methods: {
            ...mapActions([
                'getRepoListAll',
                'createPlan',
                'getPlanDetail',
                'updatePlan'
            ]),
            formatDate,
            handlePlanDetail () {
                this.isLoading = true
                this.getPlanDetail({
                    projectId: this.projectId,
                    key: this.rowsData.planId
                }).then(({
                    task: {
                        name,
                        replicaObjectType,
                        replicaType,
                        remoteClusters,
                        description,
                        createdBy,
                        createdDate,
                        setting: {
                            conflictStrategy,
                            executionStrategy,
                            executionPlan: { executeTime, cronExpression }
                        },
                        notRecord,
                        recordReserveDays
                    },
                    objects
                }) => {
                    this.planForm = {
                        ...this.planForm,
                        name,
                        executionStrategy: replicaType === 'REAL_TIME' ? 'REAL_TIME' : executionStrategy,
                        replicaObjectType,
                        ...(executeTime
                            ? {
                                time: new Date(executeTime)
                            }
                            : {}),
                        ...(cronExpression
                            ? {
                                cron: cronExpression
                            }
                            : {}),
                        conflictStrategy,
                        remoteClusterIds: remoteClusters.map(v => v.id),
                        description,
                        createdBy,
                        createdDate
                    }
                    this.replicaTaskObjects = objects
                    this.noRecordsCheck = notRecord
                    this.recordReserveDays = recordReserveDays
                }).finally(() => {
                    this.isLoading = false
                })
            },
            changeReplicaObjectType () {
                this.replicaTaskObjects = []
                this.planForm.executionStrategy === 'REAL_TIME' && (this.planForm.executionStrategy = 'IMMEDIATELY')
                this.clearError()
            },
            clearError () {
                this.$refs.planForm.clearError()
            },
            // 日志保留天数的离焦事件，用于校验输入是否符合规则
            onBlurRecordReserveDays () {
                if (isNaN(Number(this.recordReserveDays)) || this.recordReserveDays === null || this.recordReserveDays === '') {
                    this.recordReserveDays = ''
                    this.errorRecordReserveDaysInfo = true
                } else {
                    this.errorRecordReserveDaysInfo = false
                }
            },
            async save () {
                if (this.planForm.remoteClusterIds.length > 0) {
                    const clusterArr = this.clusterList.filter(v => v.type !== 'CENTER').map(v => v.id)
                    this.planForm.remoteClusterIds = this.planForm.remoteClusterIds.map(v => {
                        return clusterArr.includes(v) && v
                    }).filter(Boolean)
                }
                await this.$refs.planForm.validate()

                if (this.errorRecordReserveDaysInfo) return
                if (this.planForm.loading) return
                this.planForm.loading = true

                const replicaTaskObjects = await this.$refs.planConfig.getConfig()
                const body = {
                    name: this.planForm.name,
                    localProjectId: this.rowsData.projectId,
                    replicaObjectType: this.planForm.replicaObjectType,
                    replicaTaskObjects,
                    replicaType: this.planForm.executionStrategy === 'REAL_TIME' ? 'REAL_TIME' : 'SCHEDULED',
                    setting: {
                        rateLimit: 0, // <=0不限速
                        includeMetadata: true, // 同步元数据
                        conflictStrategy: this.planForm.conflictStrategy,
                        errorStrategy: 'CONTINUE',
                        ...(this.planForm.executionStrategy !== 'REAL_TIME'
                            ? {
                                executionStrategy: this.planForm.executionStrategy,
                                executionPlan: {
                                    executeImmediately: this.planForm.executionStrategy === 'IMMEDIATELY',
                                    ...(this.planForm.executionStrategy === 'SPECIFIED_TIME'
                                        ? {
                                            // executeTime: this.planForm.time.toISOString()
                                            // 后端需要,中国时区
                                            executeTime: new Date(this.planForm.time.getTime() + 8 * 3600 * 1000).toISOString().replace(/Z$/, '')
                                        }
                                        : {}),
                                    ...(this.planForm.executionStrategy === 'CRON_EXPRESSION'
                                        ? {
                                            cronExpression: this.planForm.cron
                                        }
                                        : {})
                                }
                            }
                            : {})
                    },
                    remoteClusterIds: this.planForm.remoteClusterIds,
                    enabled: true,
                    description: this.planForm.description
                }
                if (this.planForm?.replicaObjectType === 'REPOSITORY') {
                    body.notRecord = this.noRecordsCheck
                    // 此时需要保证传参是 number 类型
                    body.recordReserveDays = isNaN(Number(this.recordReserveDays)) ? 30 : Number(this.recordReserveDays)
                }
                const request = this.routeName === 'createPlan'
                    ? this.createPlan({ projectId: this.projectId, body })
                    : this.updatePlan({ projectId: this.projectId, body: { ...body, key: this.rowsData.planId } })
                request.then(() => {
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('save') + this.$t('success')
                    })
                    this.$emit('close')
                    this.$emit('confirm')
                }).finally(() => {
                    this.planForm.loading = false
                })
            }
        }
    }
</script>
<style lang="scss" scoped>
.create-node-container {
    height: 100%;
    background-color: white;
    overflow-y: auto;
    .plan-form {
        max-width: 1080px;
        margin-top: 30px;
        .arrow-right-icon {
            position: relative;
            width: 20px;
            height: 20px;
            &:before {
                position: absolute;
                content: '';
                width: 16px;
                height: 5px;
                margin: 8px 0;
                border-width: 1px 0;
                border-style: solid;
            }
            &:after {
                position: absolute;
                content: '';
                width: 10px;
                height: 10px;
                margin-left: 7px;
                margin-top: 6px;
                border-width: 1px 1px 0 0;
                border-style: solid;
                transform: rotate(45deg);
            }
        }
        .plan-object-container {
            display: grid;
            grid-template: auto / 1fr 1fr;
            margin: 5px 0 20px;
        }
        .radio-flex {
            height: 32px;
            display: flex;
            align-items: center;
            ::v-deep .bk-form-radio {
                display: flex;
                align-items: center;
                height: 32px;
                min-width: 80px;
                .bk-radio-text {
                    height: 32px;
                    display: flex;
                    align-items: center;
                    .label-span {
                        width: 60px;
                    }
                }
            }
        }
        ::v-deep .bk-form-radio {
            min-width: 80px;
            margin-right: 20px;
        }
        .icon-question-circle-shape {
            color: var(--fontSubsidiaryColor);
        }
    }
}
</style>

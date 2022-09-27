<template>
    <canway-dialog
        v-model="value"
        title="添加制品到白名单"
        :width="520"
        :height-num="400"
        @cancel="handleClickCancel"
    >
        <bk-form class="mr10" :label-width="90">
            <bk-select
                class="w250"
                v-model="form.artifactType"
                placeholder="请选择制品类型"
                :clearable="false">
                <bk-option v-for="item in artifactTypeOptions" :key="item.type" :id="item.type" :name="item.name"></bk-option>
            </bk-select>
            <div class="artifact-copy">
                <p class="tips">{{ mavenCreate.subTitle }}</p>
                <code-area class="mb20" :code-list="mavenCreate.codeList"></code-area>
            </div>
        </bk-form>
        <template #footer>
            <bk-button theme="default" @click="handleClickCancel">{{$t('cancel')}}</bk-button>
            <bk-button theme="primary" @click="handleClickConfirm">{{$t('confirm')}}</bk-button>
        </template>
    </canway-dialog>
</template>

<script>
    import CodeArea from '@repository/components/CodeArea'
    import repoGuideMixin from '@repository/views/repoCommon/repoGuideMixin'
    export default {
        components: {
            CodeArea
        },
        mixins: [repoGuideMixin],
        props: {
            value: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                form: {
                    artifactType: ''
                },
                artifactTypeOptions: [
                    {
                        name: 'Maven',
                        type: 'Maven'
                    }
                ],
                mavenCreate: {
                    subTitle: '请在合适的位置创建工作目录，并在该目录下执行命令：',
                    codeList: [
                        '<servers>',
                        '       <server>',
                        '               <id>我是id</id>',
                        '               <username>我是用户名</username>',
                        '               <password><PERSONAL_ACCESS_TOKEN></password>',
                        '       </server>',
                        '</servers>'
                    ]
                }
            }
        },
        methods: {
            // 关闭弹框
            handleClickCancel () {
                this.$emit('close')
            },
            // 提交弹框
            handleClickConfirm () {
                this.$$emit('confirm')
            }
        }
    }
</script>

<style lang="scss" scoped></style>

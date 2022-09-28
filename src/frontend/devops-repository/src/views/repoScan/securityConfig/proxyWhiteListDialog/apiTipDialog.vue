<template>
    <canway-dialog
        :value="show"
        title="API使用方法"
        :width="520"
        :height-num="400"
        @cancel="$emit('close')"
    >
        <bk-form class="mr10" :label-width="90">
            <bk-select
                class="w250"
                v-model="form.type"
                placeholder="请选择制品类型"
                :clearable="false">
                <bk-option v-for="item in artifactTypeList" :key="item" :id="item" :name="item" />
            </bk-select>
            <div class="artifact-copy">
                <p class="tips">请在合适的位置创建工作目录，并在该目录下执行命令：</p>
                <code-area class="mb20" :code-list="codeList"></code-area>
            </div>
        </bk-form>
        <template #footer>
            <bk-button theme="default" @click="$emit('close')">{{$t('close')}}</bk-button>
        </template>
    </canway-dialog>
</template>

<script>
    import CodeArea from '@repository/components/CodeArea'
    import repoGuideMixin from '@repository/views/repoCommon/repoGuideMixin'
    import { mapState } from 'vuex'
    import getCodeList from './getCodeList'

    export default {
        components: {
            CodeArea
        },
        mixins: [repoGuideMixin],
        props: {
            show: {
                type: Boolean,
                required: true
            }
        },
        data () {
            return {
                form: {
                    type: ''
                },
                codeList: getCodeList()
            }
        },
        computed: {
            ...mapState(['artifactTypeList'])
        },
        watch: {
            show (val) {
                if (val) {
                    this.$set(this.form, 'type', this.artifactTypeList?.[0] || '')
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
    .code-area {
        height: 200px;
        overflow: auto;
    }
</style>

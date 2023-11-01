<template>
    <canway-dialog
        :value="show"
        :title="$t('apiUseMethod')"
        :width="720"
        :height="400"
        @cancel="$emit('close')"
    >
        <bk-form class="mr10" :label-width="90">
            <!-- <bk-select
                class="w250"
                v-model="form.type"
                :placeholder="$t('artifactTypePlaceholder')"
                :clearable="false">
                <bk-option v-for="item in artifactTypeList" :key="item" :id="item" :name="item" />
            </bk-select> -->
            <div class="artifact-copy">
                <p class="tips">{{$t('apiUseCommandTip')}}</p>
                <div id="markdown-tip" />
            </div>
        </bk-form>
        <template #footer>
            <bk-button theme="default" @click="$emit('close')">{{$t('close')}}</bk-button>
        </template>
    </canway-dialog>
</template>

<script>
    import repoGuideMixin from '@repository/views/repoCommon/repoGuideMixin'
    import { mapState } from 'vuex'
    import getMarkdownTip from './getMarkdownTip'
    import getMarkdownTipEn from './getMarkdownTipEn'

    export default {
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
                }
            }
        },
        computed: {
            ...mapState(['artifactTypeList']),
            markdownTip () {
                return this.currentLanguage === 'zh-cn' ? getMarkdownTip() : getMarkdownTipEn()
            }
        },
        watch: {
            show (val) {
                if (val) {
                    this.$set(this.form, 'type', this.artifactTypeList?.[0] || '')
                }
            }
        },
        mounted () {
            // eslint-disable-next-line no-undef
            return new Cherry({
                id: 'markdown-tip',
                value: this.markdownTip,
                toolbars: {
                    toolbar: false
                },
                editor: {
                    defaultModel: 'previewOnly'
                },
                engine: {
                    syntax: {
                        codeBlock: {
                            theme: 'light'
                        }
                    }
                }
            })
        }
    }
</script>

<style lang="scss" scoped>
    #markdown-tip {
        max-height: 400px;
        overflow: auto;
    }
</style>

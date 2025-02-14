
<!-- sbt 无法选择，也不能pull -->
<template>
    <canway-dialog
        v-model="useGuideData.show"
        :width="850"
        :height="470"
        :show-footer="false"
        render-directive="if"
        @cancel="cancel">
        <template #header>
            <div class="guide-dialog-header flex-align-center">
                <icon class="mr5" :name="isSbt ? 'sbt' : repoType" size="32"></icon>
                <span>{{ repoName + $t('space') + title }}</span>
                <icon v-if="isSbt" class="ml5" :name="repoType" size="12"></icon>
            </div>
        </template>
        <div class="guide-container">
            <!-- 左侧tab标签页 -->
            <div class="guide-side w160">
                <bk-select
                    v-if="['maven', 'npm', 'gradle'].includes(repoType) && !isSbt"
                    class="w160"
                    :clearable="false"
                    v-model="activeDependType">
                    <bk-option v-for="type in dependTypes" :key="type" :id="type" :name="type">
                        <div class="flex-align-center">
                            <!-- <Icon size="20" :name="type" /> -->
                            <span class="ml10 flex-1 text-overflow">{{type}}</span>
                        </div>
                    </bk-option>
                </bk-select>
                <div class="mt10" v-for="(option, index) in guideOption" :key="index">
                    <bk-button class="w160" :theme="option.name === activeTab ? 'primary' : ''" @click="onChangeOption(option.name)">{{option.label}}</bk-button>
                </div>
            </div>
            <!-- 右侧具体使用指引 -->
            <div class="guide-main ml10">
                <repo-guide ref="repoGuideRefs" v-if="activeTab.length > 0" class="pl20 pr20 pb20" :article="articleGuide" :option-type="activeTab" :construct-type="activeDependType"></repo-guide>
            </div>
        </div>
    </canway-dialog>
</template>
<script>
    import repoGuide from '@repository/views/repoCommon/repoGuide'
    import repoGuideMixin from '@repository/views/repoCommon/repoGuideMixin'
    import { mapMutations, mapActions } from 'vuex'

    export default {
        name: 'useGuide',
        components: { repoGuide },
        mixins: [repoGuideMixin],
        props: {
            title: {
                type: String,
                default () {
                    return this.$t('guide')
                }
            }
        },
        data () {
            return {
                useGuideData: {
                    show: false,
                    loading: false
                },
                activeTab: 'setCredentials',
                activeDependType: '',
                repoInfo: {}
            }
        },
        computed: {
            // 使用指引的可用操作，需要根据仓库类型及制品类型等修改
            guideOption () {
                let basicOption = [
                    { name: 'setCredentials', label: this.$t('setCredentials') },
                    { name: 'pull', label: this.$t('pull') }
                ]
                // go远程仓库，且为http协议的情况下
                if (this.repoType === 'go' && this.storeType === 'remote' && this.removeProtocolFromUrl(this.repoUrl, true) === 'http') {
                    basicOption = basicOption.filter(item => item.name !== 'setCredentials')
                }
                
                // ivy仓库不支持pull操作
                if (this.repoType === 'ivy' || this.isSbt) {
                    basicOption = basicOption.filter(item => item.name !== 'pull')
                }
                if (!this.noShowOption) {
                    basicOption.push({ name: 'push', label: this.$t('push') })
                }
                if (this.repoType === 'nuget' && this.storeType !== 'virtual' && !this.whetherSoftware) {
                    basicOption.push({ name: 'delete', label: this.$t('deleteArtifact') })
                }
                return basicOption
            },
            // 构建工具的下拉选择项，目前只有maven及npm仓库才支持
            dependTypes () {
                let types = []
                if (['maven', 'gradle'].includes(this.repoType)) {
                    types = ['Gradle Groovy DSL', 'Gradle Kotlin DSL']
                    if (this.repoType === 'gradle') {
                        types.push('Apache Maven')
                    } else if (this.repoType === 'maven') {
                        types.unshift('Apache Maven')
                    }
                } else if (this.repoType === 'npm') {
                    types = ['npm', 'yarn']
                }
                this.activeDependType = types?.length > 0 ? types[0] : ''
                return types
            }
        },
        watch: {
            'useGuideData.show': {
                handler (val) {
                    if (val) {
                        this.$nextTick(() => {
                            if (this.guideOption.find(item => item.name !== 'setCredentials')) {
                                this.activeTab = this.guideOption[0].name
                            }
                            if (this.repoType === 'ivy') {
                                this.getRepoDetail()
                            }
                        })
                    }
                },
                deep: true,
                immediate: true
            }
        },
        methods: {
            ...mapMutations(['SET_DEPEND_ACCESS_TOKEN_VALUE']),
            ...mapActions(['getRepoInfo']),
            getRepoDetail () {
                this.getRepoInfo({
                    projectId: this.projectId,
                    repoName: this.repoName,
                    repoType: this.repoType
                }).then(res => {
                    this.repoInfo = res
                })
            },
            setData (data) {
                this.useGuideData = {
                    ...this.useGuideData,
                    ...data
                }
            },
            cancel () {
                this.useGuideData.show = false
                // 关闭弹窗时将操作重新切换为设置凭证
                this.activeTab = 'setCredentials'
                this.SET_DEPEND_ACCESS_TOKEN_VALUE('')
                this.$refs.repoGuideRefs.resetInputValue()
            },
            onChangeOption (name) {
                this.activeTab = name
            }
        }
    }
</script>
<style lang="scss" scoped>
.guide-dialog-header {
    font-weight: 800;
    font-size: 16px;
}
.w160 {
    width: 160px;
}
.guide-container {
    display: flex;
    height: 470px;
    overflow: hidden;
    .guide-side {
        height: 470px;
    }
    .guide-main {
        width: calc(100% - 170px);
        overflow-y: auto;
        border-left: 1px solid var(--borderColor);
    }
}
</style>

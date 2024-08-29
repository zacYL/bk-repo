<template>
    <div class="repo-guide-container">
        <!-- 当前选择的操作是 设置凭证 时，需要显示 访问令牌 相关元素 -->
        <template v-if="optionType === 'setCredentials'">
            <div class="section-header">
                {{ $t('token') }}
            </div>
            <div class="pt10 pb10">
                {{ $t('clickInfo') + $t('space') }}
                <bk-button text theme="primary" @click="createToken">{{ $t('createToken') }}</bk-button>
                {{ $t('tokenDependSubTitle') }}
                <bk-button v-if="ciMode" text theme="primary" @click="jumpCCommonUserToken">{{ $t('token') }}</bk-button>
                <router-link v-else :to="{ name: 'repoToken' }">{{ $t('token') }}</router-link>
            </div>
            <div v-if="repoType !== 'npm'">
                <div>{{$t('accessTokenPlaceholder')}}</div>
                <bk-input
                    class="mt10"
                    v-model.trim="tokenInput"
                    :placeholder="$t('accessTokenPlaceholder')"
                    clearable
                    @change="onChangeAccessToken">
                </bk-input>
            </div>
            <ci-create-token-dialog v-if="ciMode" ref="ciCreateToken"></ci-create-token-dialog>
            <create-token-dialog v-else ref="createToken"></create-token-dialog>
        </template>
        <template v-if="currentArticleList.length > 0">
            <div v-for="(currentArticle,cIndex) in currentArticleList" :key="cIndex">
                <bk-alert
                    v-if="currentArticle.showErrTips"
                    class="mb20"
                    style="height: unset;width: 100%;"
                    type="warning"
                    :title="currentArticle.errTips">
                </bk-alert>
                <div v-if="currentArticle.title" class="section-header pt10">
                    {{ currentArticle.title }}
                </div>
                <!-- 当构建类型constructType和当前对象的constructType一致，且当前对象的notShowArtifactInput为true时不需要显示制品名称等的输入框，暂时只有 Apache Maven不显示 -->
                <template v-if="(currentArticle.inputBoxList || []).length > 0 && !currentArticle.main.find(item => item.constructType === constructType && item.notShowArtifactInput)">
                    <div class="mt10"> {{$t('guideInputInfo', { option: currentArticle.title })}}</div>
                    <div v-for="(box,index) in currentArticle.inputBoxList" :key="box.key || box">
                        <div class="flex-align-center mt20 mb10">
                            <span style="width:120px;">{{box.label}}: </span>
                            <bk-input
                                class="w480"
                                v-model.trim="boxValues[index]"
                                :placeholder="box.placeholder"
                                clearable
                                @change="onChangeBoxValue(box.methodFunctionName,boxValues[index])">
                            </bk-input>
                        </div>
                    </div>
                </template>
                <div class="section-main flex-column" v-for="block in currentArticle.main" :key="block.subTitle">
                    <!--    传参的 constructType === 当前json对象的 constructType时、
                            当前json对象不存在 constructType 这个字段、
                            当前json对象的 constructType 字段的值为 common，表示公用，
                            这三种情况下需要显示下面具体指引
                    -->
                    <template v-if="(constructType === block.constructType) || !block.constructType || (block.constructType === 'common') ">
                        <span v-if="block.title" class="section-header pt10">{{ block.title }}</span>
                        <span v-if="block.subTitle" class="sub-title pt10 fw500" :style="block.subTitleStyle">{{ block.subTitle }}</span>
                        <div v-if="block?.contentList?.length">
                            <p v-for="(content, index) in block.contentList" :key="index" :class="(typeof content === 'string' ? '' : content.class)" style="color: #8797aa;">
                                {{ typeof content === 'string' ? content : content.val }}
                            </p>
                        </div>
                        <code-area :class="[block.codeNoMargin ? '' : 'mt15', block.codeClass]" v-if="block.codeList && block.codeList.length" :code-list="block.codeList"></code-area>
                    </template>
                    <div v-if="block?.components?.length" :style="{
                        display: block.componentInline ? 'flex' : ''
                    }">
                        <div v-for="(component, index) in block.components" :key="index">
                            <bk-select
                                v-if="component.type === 'select'"
                                @change="component.cb"
                                style="width: 200px;"
                                class="mt5 mb5 mr5"
                            >
                                <bk-option v-for="option in component.values"
                                    :key="option.downloadUrl"
                                    :id="option.downloadUrl"
                                    :name="option.platform">
                                </bk-option>
                            </bk-select>
                            <bk-button
                                class="mt5 mb5 mr5"
                                v-if="component.type === 'button'"
                                :disabled="component.disabled"
                                @click="component.cb"
                                style="width: fit-content;"
                            >
                                {{ $t('download') }}
                            </bk-button>
                        </div>
                    </div>
                </div>
            </div>
        </template>
    </div>
</template>
<script>
    import CodeArea from '@repository/components/CodeArea'
    import createTokenDialog from '@repository/views/repoToken/createTokenDialog'
    import ciCreateTokenDialog from '@repository/views/repoToken/ciCreateTokenDialog'
    import { mapState, mapMutations } from 'vuex'
    export default {
        name: 'repoGuide',
        components: { CodeArea, createTokenDialog, ciCreateTokenDialog },
        props: {
            article: {
                type: Array,
                default: () => []
            },
            optionType: {
                type: String,
                required: true,
                describe: '当前选择的操作类型,例如设置凭证()、推送(push)、拉取(pull)、删除(delete)等'
            },
            constructType: {
                type: String,
                default: '',
                describe: '当前选择的构建工具类型，目前只限于maven和npm仓库中才有此类型，其他的为空字符串'
            }
        },
        data () {
            return {
                ciMode: MODE_CONFIG === 'ci',
                tokenInput: '',
                boxValues: [] // 使用指引中输入框v-model绑定的值所在的数组，取值的时候通过下标获取
            }
        },
        computed: {
            ...mapState(['dependInputValue1', 'dependInputValue2']),
            currentArticleList () {
                return this.article.map(item => {
                    return (item.optionType === this.optionType) ? item : ''
                }).filter(Boolean) || []
            },
            repoType () {
                return this.$route.params.repoType || ''
            }
        },
        watch: {
            optionType: {
                handler (value) {
                    // 此处需要将上一个操作中输入框中的vuex中的值清空(访问令牌输入框的信息需要保留，其他操作可能要用到)，防止不同操作之间互相影响
                    this.resetInputValue()
                    // 注意：此时需要将输入框v-model绑定的值清空，否则会导致切换了操作之后输入框数据依旧存在
                    this.boxValues = []
                },
                immediate: true
            }
        },
        methods: {
            // ...mapMutations(['SET_DEPEND_ACCESS_TOKEN_VALUE', ...this.currentArticle.inputBoxList.map(item => item.methodFunctionName)]),
            ...mapMutations(['SET_DEPEND_ACCESS_TOKEN_VALUE', 'SET_DEPEND_INPUT_VALUE1', 'SET_DEPEND_INPUT_VALUE2', 'SET_DEPEND_INPUT_VALUE3']),
            createToken () {
                this.ciMode ? this.$refs.ciCreateToken.showDialogHandler() : this.$refs.createToken.showDialogHandler()
            },
            // 集成CI模式下需要跳转到用户个人中心的访问令牌页面
            jumpCCommonUserToken () {
                window.open(window.DEVOPS_SITE_URL + '/console/userCenter/userToken', '_blank')
            },
            onChangeAccessToken () {
                this.SET_DEPEND_ACCESS_TOKEN_VALUE(this.tokenInput)
            },
            onChangeBoxValue (functionName, value) {
                this[functionName](value)
            },
            // 重置所有的输入框为空字符串
            resetInputValue () {
                this.SET_DEPEND_INPUT_VALUE1('')
                this.SET_DEPEND_INPUT_VALUE2('')
                this.SET_DEPEND_INPUT_VALUE3('')
            }
        }
    }
</script>
<style lang="scss" scoped>
.fw500 {
    font-weight: 500;
}
.repo-guide-container {
   position: relative;
    .section-header {
        padding-left: 10px;
        color: var(--fontPrimaryColor);
        font-weight: bold;
        font-size: 14px;
        &:before {
            position: absolute;
            left: 20px;
            content: "";
            width: 3px;
            height: 12px;
            background-color: var(--primaryColor);
            margin-top: 5px;
        }
    }
}
</style>

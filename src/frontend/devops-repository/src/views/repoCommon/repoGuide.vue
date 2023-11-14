<template>
    <div class="repo-guide-container">
        <!-- 当前选择的操作是 设置凭证 时，需要显示 访问令牌 相关元素 -->
        <template v-if="currentArticle.optionType === 'setCredentials'">
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
            <div>
                <div>{{$t('accessTokenPlaceholder')}}</div>
                <bk-input
                    class="mt5 mb5"
                    v-model.trim="tokenInput"
                    type="password"
                    :placeholder="$t('accessTokenPlaceholder')"
                    clearable
                    @change="onChangeAccessToken">
                </bk-input>
            </div>
            <ci-create-token-dialog v-if="ciMode" ref="ciCreateToken"></ci-create-token-dialog>
            <create-token-dialog v-else ref="createToken"></create-token-dialog>
        </template>
        <div v-if="currentArticle.title" class="section-header">
            {{ currentArticle.title }}
        </div>
        <div class="section-main flex-column" v-for="block in currentArticle.main" :key="block.subTitle">
            <!--    传参的 constructType === 当前json对象的 constructType时、
                    当前json对象不存在 constructType 这个字段、
                    当前json对象的 constructType 字段的值为 common，表示公用，
                    这三种情况下需要显示下面具体指引
            -->
            <template v-if="(constructType === block.constructType) || !block.constructType || (block.constructType === 'common') ">
                <span v-if="block.subTitle" class="sub-title pt10" :style="block.subTitleStyle">{{ block.subTitle }}</span>
                <code-area class="mt15" v-if="block.codeList && block.codeList.length" :code-list="block.codeList"></code-area>
            </template>
        </div>
    </div>
</template>
<script>
    import CodeArea from '@repository/components/CodeArea'
    import createTokenDialog from '@repository/views/repoToken/createTokenDialog'
    import ciCreateTokenDialog from '@repository/views/repoToken/ciCreateTokenDialog'
    import { mapMutations } from 'vuex'
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
                tokenInput: ''
            }
        },
        computed: {
            currentArticle () {
                return this.article.find(item => item.optionType === this.optionType)
            }
        },
        methods: {
            ...mapMutations(['SET_DEPEND_ACCESS_TOKEN_VALUE']),
            createToken () {
                this.ciMode ? this.$refs.ciCreateToken.showDialogHandler() : this.$refs.createToken.showDialogHandler()
            },
            // 集成CI模式下需要跳转到用户个人中心的访问令牌页面
            jumpCCommonUserToken () {
                window.open(window.DEVOPS_SITE_URL + '/console/userCenter/userToken', '_blank')
            },
            onChangeAccessToken () {
                this.SET_DEPEND_ACCESS_TOKEN_VALUE(this.tokenInput)
            }
        }
    }
</script>
<style lang="scss" scoped>
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

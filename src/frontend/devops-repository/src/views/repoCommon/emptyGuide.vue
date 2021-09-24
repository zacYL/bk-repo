<template>
    <div class="empty-guide-container">
        <div class="pt40 pb40 empty-guide-header flex-center flex-column">
            <div class="mb20 empty-guide-title">仓库暂无制品</div>
            <div class="empty-guide-subtitle">
                <span>请参考下方使用指引来推送您的第一个制品</span>
                <template v-if="showRepoConfigRoute">
                    <span>，或者</span>
                    <router-link class="router-link" :to="{ name: 'repoConfig', query: { repoName: this.$route.query.repoName } }">配置代理</router-link>
                    <span>以代理其他仓库的制品</span>
                </template>
            </div>
        </div>
        <div class="empty-guide-main">
            <div class="empty-guide-item">
                <div class="guide-step">
                    <span class="step-count">step</span>
                </div>
                <header class="empty-guide-item-title">{{ $t('token') }}</header>
                <div class="empty-guide-item-main flex-between-center">
                    <div class="empty-guide-item-subtitle">
                        {{ $t('tokenSubTitle') }}
                        <router-link class="router-link" :to="{ name: 'repoToken' }">{{ $t('token') }}</router-link>
                    </div>
                    <bk-button class="mt15" theme="primary" @click="createToken">{{ $t('createToken') }}</bk-button>
                </div>
                <create-token-dialog ref="createToken"></create-token-dialog>
            </div>
            <div class="empty-guide-item" v-for="(section, index) in article" :key="`section${index}`">
                <div class="guide-step">
                    <span class="step-count">step</span>
                </div>
                <header v-if="section.title" class="empty-guide-item-title">{{ section.title }}</header>
                <div class="empty-guide-item-main">
                    <div v-for="block in section.main" :key="block.subTitle">
                        <div v-if="block.subTitle" class="ml20 empty-guide-item-subtitle" :style="block.subTitleStyle">{{ block.subTitle }}</div>
                        <code-area class="mt15" bg-color="#e6edf6" color="#081e40" v-if="block.codeList && block.codeList.length" :code-list="block.codeList"></code-area>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>
<script>
    import CodeArea from '@/components/CodeArea'
    import createTokenDialog from '@/views/repoToken/createTokenDialog'
    export default {
        name: 'emptyGuide',
        components: { CodeArea, createTokenDialog },
        props: {
            article: {
                type: Array,
                default: () => []
            }
        },
        computed: {
            showRepoConfigRoute () {
                return ['maven', 'pypi', 'npm', 'composer', 'nuget'].includes(this.$route.params.repoType)
            }
        },
        methods: {
            createToken () {
                this.$refs.createToken.showDialogHandler()
            }
        }
    }
</script>
<style lang="scss" scoped>
$bgColor: #f1f8ff;
.empty-guide-container {
    padding: 10px 60px;
    position: relative;
    .empty-guide-header {
        position: sticky;
        top: -137px;
        z-index: 1;
        color: var(--fontBoldColor);
        background-color: white;
        .empty-guide-title {
            font-size: 16px;
            font-weight: bold;
            color: var(--fontBoldColor);
        }
        .empty-guide-subtitle {
            font-size: 12px;
            color: #385377;
        }
    }
    .empty-guide-main {
        padding: 20px 50px 0;
        border: 1px dashed var(--borderWeightColor);
        border-radius: 4px;
        counter-reset: step;
        .empty-guide-item {
            --marginBottom: 20px;
            position: relative;
            margin-left: 80px;
            margin-bottom: var(--marginBottom);
            padding: 20px;
            background-color: var(--bgHoverColor);
            .guide-step {
                position: absolute;
                left: -80px;
                top: 30px;
                height: calc(100% + var(--marginBottom));
                border-left: 1px dashed var(--primaryColor);
                &:before {
                    content: '';
                    position: absolute;
                    width: 10px;
                    height: 10px;
                    margin: -12px 0 0 -12px;
                    border: 6px solid #d7e6ff;
                    background-color: var(--primaryColor);
                    border-radius: 50%;
                }
                .step-count {
                    position: absolute;
                    margin-left: 30px;
                    margin-top: 5px;
                    &:before {
                        position: absolute;
                        content: '0';
                        margin-top: -20px;
                        font-size: 20px;
                    }
                    &:after {
                        position: absolute;
                        counter-increment: step;
                        content: counter(step);
                        margin-top: -20px;
                        margin-left: -15px;
                        font-size: 20px;
                    }
                }
            }
            &:last-child {
                .guide-step {
                    height: 0;
                }
            }
        }
        .empty-guide-item-title {
            position: relative;
            color: var(--fontBoldColor);
            font-size: 16px;
            font-weight: bold;
        }
        .empty-guide-item-main {
            .empty-guide-item-subtitle {
                position: relative;
                padding-top: 15px;
            }
        }
    }
}
</style>

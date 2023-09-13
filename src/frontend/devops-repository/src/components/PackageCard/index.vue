<template>
    <div class="package-card-container flex-align-center">
        <Icon class="mr20 card-icon" size="70" :name="cardData.type ? cardData.type.toLowerCase() : getIconName(cardData.name)" />
        <div class="mr20 package-card-main flex-column">
            <div class="flex-align-center">
                <span class="card-name text-overflow" :title="cardData.name">{{ cardData.name }}</span>
                <span class="ml10 repo-tag" v-if="['MAVEN'].includes(cardData.type)">{{ cardData.key.replace(/^.*\/\/(.+):.*$/, '$1') }}</span>
                <scan-tag class="ml10"
                    v-if="isEnterprise && !cardData.type && genericScanFileTypes.includes(cardData.name.replace(/^.+\.([^.]+)$/, '$1'))"
                    :status="(cardData.metadata || {}).scanStatus"
                    readonly>
                </scan-tag>
                <forbid-tag class="ml10"
                    v-if="((cardData.metadata || {}).forbidStatus || cardData.forbidStatus)"
                    v-bind="cardData.metadata">
                </forbid-tag>
            </div>
            <span class="package-card-description text-overflow" :title="cardData.description">{{ cardData.description }}</span>
            <div class="package-card-data" :style="dynamicStyle">
                <!-- 依赖源仓库 -->
                <template v-if="cardData.type">
                    <div class="card-metadata" :title="`最新版本：${cardData.latest}`">
                        <scan-tag class="ml5"
                            v-if="isEnterprise && showRepoScan"
                            :status="cardData.scanStatus"
                            readonly>
                        </scan-tag>
                    </div>
                    <div class="card-metadata" :title="$t('lastModifiedDate') + `：${formatDate(cardData.lastModifiedDate)}`"></div>
                    <div v-if="whetherRepoSearch" class="card-metadata" :title="$t('repo') + `：${cardData.repoName}`"></div>
                    <div class="card-metadata" :title="`版本数：${cardData.versions}`"></div>
                    <div class="card-metadata" :title="`下载统计：${cardData.downloads}`"></div>
                    <div v-if="whetherRepoVirtual" class="card-metadata" :title="`仓库来源：${cardData.repoName}`"></div>
                    <div v-if="showRepoSearchVersion" class="card-metadata" :title="$t('searchVersionResultInfo') + `： ${cardData.matchedVersions.length}`"></div>
                </template>
                <!-- generic 仓库 -->
                <template v-else>
                    <div class="card-metadata" :title="$t('repo') + `：${cardData.repoName}`"></div>
                    <div class="card-metadata" :title="`文件大小：${convertFileSize(cardData.size)}`"></div>
                    <div class="card-metadata" :title="$t('lastModifiedDate') + `：${formatDate(cardData.lastModifiedDate)}`"></div>
                </template>
            </div>
        </div>
        <div class="card-operation flex-center">
            <Icon class="hover-btn" v-if="!readonly && !whetherRepoVirtual" size="24" name="icon-delete" @click.native.stop="deleteCard" />
            <operation-list
                v-if="!cardData.type && !readonly"
                :list="[
                    { label: '详情', clickEvent: () => detail() },
                    !(cardData.metadata || {}).forbidStatus && { label: '下载', clickEvent: () => download() },
                    !(cardData.metadata || {}).forbidStatus && { label: '共享', clickEvent: () => share() }
                ]"></operation-list>
        </div>
    </div>
</template>
<script>
    import OperationList from '@repository/components/OperationList'
    import ScanTag from '@repository/views/repoScan/scanTag'
    import forbidTag from '@repository/components/ForbidTag'
    import { mapGetters } from 'vuex'
    import { convertFileSize, formatDate } from '@repository/utils'
    import { getIconName, scanTypeEnum, genericScanFileTypes } from '@repository/store/publicEnum'
    export default {
        name: 'packageCard',
        components: { OperationList, ScanTag, forbidTag },
        props: {
            cardData: {
                type: Object,
                default: {}
            },
            readonly: {
                type: Boolean,
                default: false
            },
            // 是否需要展示版本相关信息，展示搜索结果
            showSearchVersionList: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                genericScanFileTypes
            }
        },
        computed: {
            ...mapGetters(['isEnterprise']),
            showRepoScan () {
                return Object.keys(scanTypeEnum).join(',').includes(this.cardData.type)
            },
            // 当前仓库类型
            storeType () {
                return this.$route.query.storeType || ''
            },
            // 是否是搜索状态，即是否需要显示搜索结果等字段
            showRepoSearchVersion () {
                return this.showSearchVersionList && (this.cardData.matchedVersions || []).length > 0
            },
            // 是否是制品搜索页面
            whetherRepoSearch () {
                return this.$route.path.endsWith('repoSearch')
            },
            // 是否是虚拟仓库
            whetherRepoVirtual () {
                return this.storeType === 'virtual'
            },
            // grid 布局根据依赖源、搜索、虚拟仓库等 需要拆分为 几部分
            dynamicStyle () {
                let style = 4
                if (this.whetherRepoSearch) {
                    // 在制品搜索页面时
                    if (this.cardData.type) {
                        // 依赖源仓库，添加了所属仓库，所以默认值为 4 + 1 = 5
                        // 搜索条件有值的状态下，因为添加了搜索结果字段，需要加 1
                        style = this.showRepoSearchVersion ? 6 : 5
                    } else {
                        // generic 仓库
                        style = 3
                    }
                } else {
                    // 非搜索页面，因为generic仓库没有使用到此组件，只有依赖源仓库在用，所以最小为 4
                    // 虚拟仓库，因为添加了仓库来源，需要加 1
                    style = this.whetherRepoVirtual ? 5 : 4
                }
                return { 'grid-template': `auto / repeat( ${style}, 1fr)` }
            }
        },
        methods: {
            convertFileSize,
            formatDate,
            getIconName,
            deleteCard () {
                this.$emit('delete-card')
            },
            detail () {
                this.$emit('show-detail', this.cardData)
            },
            download () {
                const url = `/generic/${this.cardData.projectId}/${this.cardData.repoName}/${encodeURIComponent(this.cardData.fullPath)}?download=true`
                this.$ajax.head(url).then(() => {
                    window.open(
                        '/web' + url,
                        '_self'
                    )
                }).catch(e => {
                    const message = e.status === 423 ? this.$t('fileDownloadError') : this.$t('fileError')
                    this.$bkMessage({
                        theme: 'error',
                        message
                    })
                })
            },
            share () {
                this.$emit('share', this.cardData)
            }
        }
    }
</script>
<style lang="scss" scoped>
.package-card-container {
    flex: 1;
    height: 100px;
    padding: 16px 20px;
    border-radius: 5px;
    background-color: var(--bgLighterColor);
    cursor: pointer;
    .card-icon {
        padding: 15px;
        background-color: white;
        border: 1px solid var(--borderColor);
        border-radius: 4px;
    }
    .package-card-main {
        flex: 1;
        height: 100%;
        justify-content: space-around;
        overflow: hidden;
        .card-name {
            font-size: 14px;
            max-width: 500px;
            font-weight: bold;
            &:hover {
                color: var(--primaryColor);
            }
        }
        .package-card-description {
            font-size: 12px;
            color: var(--fontSubsidiaryColor);
        }
        .package-card-data {
            display: grid;
            // grid-template: auto / repeat(6, 1fr);
            .card-metadata {
                display: flex;
                align-items: center;
                padding-right: 10px;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
                &:before {
                    content: attr(title);
                    color: var(--fontSubsidiaryColor);
                }
            }
        }
    }
    .card-operation {
        visibility: hidden;
        flex-basis: 50px;
    }
    &:hover {
        background-color: var(--bgHoverLighterColor);
        // box-shadow: 0px 0px 6px 0px var(--primaryBoxShadowColor);
        .repo-tag {
            border-color: var(--primaryBoxShadowColor);
        }
        .card-operation {
            visibility: visible;
        }
    }
}
</style>

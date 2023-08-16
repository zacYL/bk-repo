<template>
    <div class="package-card-container flex-align-center">
        <Icon class="mr20 card-icon" size="70" :name="cardData.type ? cardData.type.toLowerCase() : getIconName(cardData.name)" />
        <div class="mr20 package-card-main flex-column">
            <div class="flex-align-center">
                <span class="card-name text-overflow" :title="cardData.name">{{ cardData.name }}</span>
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
            <div class="package-card-data">
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
                    <div class="card-metadata" :title="`版本数：${cardData.versions}`"></div>
                    <div class="card-metadata" :title="`下载统计：${cardData.downloads}`"></div>
                    <div v-if="storeType === 'virtual'" class="card-metadata" :title="`仓库来源：${cardData.repoName}`"></div>
                    <div v-if="showRepoSearchVersion" class="card-metadata" :title="$t('searchVersionResultInfo') + `： ${cardData.matchedVersions.length}`"></div>
                </template>
                <!-- generic 仓库 -->
                <template v-else>
                    <div class="card-metadata" :title="`所属仓库：${cardData.repoName}`"></div>
                    <div class="card-metadata" :title="`文件大小：${convertFileSize(cardData.size)}`"></div>
                    <div class="card-metadata" :title="$t('lastModifiedDate') + `：${formatDate(cardData.lastModifiedDate)}`"></div>
                </template>
            </div>
        </div>
        <div class="card-operation flex-center">
            <Icon class="hover-btn" v-if="!readonly && !(storeType === 'virtual') " size="24" name="icon-delete" @click.native.stop="deleteCard" />
            <bk-popover
                v-if="showRepoSearchVersion"
                v-focus
                ref="popoverVersionsRef"
                placement="left-start"
                theme="light"
                :max-width="210"
                :width="210"
                :arrow="false"
                :distance="0"
                trigger="click"
                :on-show="() => {
                    showSearchVersionListPopover = true
                } "
                :on-hide="() => {
                    showSearchVersionListPopover = false
                } ">
                <Icon class="hover-btn" size="24" name="portrait-more" @click.native.stop="onClickSearchMoreIcon" />
                <template #content>
                    <div class="search-version-container">
                        <span>{{$t('searchCheckVersionToDetail')}}</span>
                        <div v-for="version in cardData.matchedVersions" :key="version">
                            <span
                                class="search-version-item text-overflow pl10"
                                :title="(version || '').length > 13 ? version : ''"
                                @click.stop="onJumpToSpecificVersion(version)">
                                {{version}}
                            </span>
                        </div>
                    </div>
                </template>
            </bk-popover>
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
            // 是否需要展示版本相关信息，展示搜索结果及更多按钮，点击显示版本列表
            showSearchVersionList: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                genericScanFileTypes,
                showSearchVersionListPopover: false
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
            showRepoSearchVersion () {
                return this.showSearchVersionList && (this.cardData.matchedVersions || []).length > 0
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
            },
            // 点击展开更多按钮可以打开弹窗，再次点击关闭弹窗
            onClickSearchMoreIcon () {
                this.$nextTick(() => {
                    this.$refs.popoverVersionsRef[this.showSearchVersionListPopover ? 'hideHandler' : 'showHandler']()
                })
            },
            // 跳转到具体的版本
            onJumpToSpecificVersion (version) {
                this.$emit('jump-to-specific-version', version)
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
            grid-template: auto / repeat(5, 1fr);
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
.search-version-container {
    max-height: 200px;
    min-height: 60px;
    overflow-y: auto;
}
.search-version-item {
    display: block;
    height: 30px;
    line-height: 30px;
    border-radius: 2px;
    background-color: var(--bgLightColor);
    cursor: pointer;
    min-width: 120px;
    max-width: 180px;
    margin-bottom:10px;
    &:first-child {
        margin-top: 10px;
    }
    &:hover{
        background-color: var(--primaryColor);;
        color: #fff;
    }
}
</style>

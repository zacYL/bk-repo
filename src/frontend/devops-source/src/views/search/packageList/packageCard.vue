<template>
    <div class="package-card-container flex-align-center" @click="showCommonPackageDetail">
        <div class="mr20 package-card-main flex-column">
            <div class="flex-align-center">
                <span class="package-card-name">{{ cardData.name }}</span>
                <span class="ml10 repo-tag" v-if="cardData.type === 'MAVEN'">{{ cardData.key.replace(/^.*\/\/(.+):.*$/, '$1') }}</span>
                <span v-if="showRepo" class="ml10 repo-tag"><icon size="14" :name="cardData.type.toLowerCase()" />{{cardData.repoName}}</span>
            </div>
            <div class="ml20 package-card-description">
                <span :title="cardData.description">{{ cardData.description || '--' }}</span>
            </div>
            <div class="package-card-data flex-align-center">
                <div class="flex-align-center" :title="`最新版本：${cardData.latest}`"><icon class="mr5" size="16" name="latest-version" />{{ cardData.latest }}</div>
                <div class="flex-align-center" :title="`版本数量：${cardData.versions}`"><icon class="mr5" size="16" name="versions" />{{ cardData.versions }}</div>
                <div class="flex-align-center" :title="`下载次数：${cardData.downloads}`"><icon class="mr5" size="16" name="downloads" />{{ cardData.downloads }}</div>
                <div class="flex-align-center"><icon class="mr5" size="16" name="time" />{{ formatDate(cardData.lastModifiedDate) }}</div>
                <div class="flex-align-center"><icon class="mr5" size="16" name="updater" />{{ userList[cardData.lastModifiedBy] ? userList[cardData.lastModifiedBy].name : cardData.lastModifiedBy }}</div>
            </div>
        </div>
        <i v-if="userInfo.admin" class="devops-icon icon-delete package-card-delete hover-btn" @click.stop="deleteCard"></i>
    </div>
</template>
<script>
    import { formatDate } from '@/utils'
    import { mapState, mapActions } from 'vuex'
    export default {
        name: 'packageCard',
        props: {
            cardData: {
                type: Object,
                default: {}
            },
            showRepo: {
                type: Boolean,
                default: false
            }
        },
        computed: {
            ...mapState(['userInfo', 'userList']),
            projectId () {
                return PROJECT_ID
            }
        },
        methods: {
            formatDate,
            ...mapActions(['deletePackage']),
            deleteCard () {
                this.$bkInfo({
                    type: 'warning',
                    theme: 'warning',
                    title: this.$t('deletePackageTitle'),
                    subTitle: this.$t('deletePackageSubTitle'),
                    showFooter: true,
                    confirmFn: () => {
                        this.deletePackage({
                            projectId: this.projectId,
                            repoType: this.cardData.type.toLowerCase(),
                            repoName: this.cardData.repoName,
                            packageKey: this.cardData.key
                        }).then(() => {
                            this.$emit('refresh')
                            this.$bkMessage({
                                theme: 'success',
                                message: this.$t('delete') + this.$t('success')
                            })
                        })
                    }
                })
            },
            showCommonPackageDetail () {
                this.$router.push({
                    name: 'searchPackageDetail',
                    params: {
                        repoType: this.cardData.type.toLowerCase(),
                        repoName: this.cardData.repoName
                    },
                    query: {
                        package: this.cardData.key
                    }
                })
            }
        }
    }
</script>
<style lang="scss" scoped>
@import '@/scss/conf';
.package-card-container {
    padding: 5px 20px;
    border: 1px solid $borderWeightColor;
    border-radius: 5px;
    background-color: #fdfdfe;
    cursor: pointer;
    &:hover {
        border-color: $iconPrimaryColor;
    }
    .package-card-main {
        flex: 1;
        height: 100%;
        justify-content: space-around;
        overflow: hidden;
        .package-card-name {
            color: #222222;
            font-size: 12px;
            font-weight: bold;
        }
        .package-card-description {
            margin: 5px 0;
            font-size: 12px;
            max-width: 800px;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            overflow: hidden;
        }
        .package-card-data {
            color: $fontWeightColor;
            font-size: 14px;
            font-weight: normal;
            div {
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
                &:nth-child(1) {
                    flex-basis: 250px;
                }
                &:nth-child(2) {
                    flex-basis: 120px;
                }
                &:nth-child(3) {
                    flex-basis: 140px;
                }
                &:nth-child(4) {
                    flex-basis: 275px;
                }
                &:nth-child(5) {
                    flex-basis: 175px;
                }
            }
        }
    }
    .package-card-delete {
        flex-basis: 30px;
        font-size: 16px;
    }
}
</style>

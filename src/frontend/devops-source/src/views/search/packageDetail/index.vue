<template>
    <div class="package-detail flex-column">
        <div class="package-info flex-align-center" v-bkloading="{ isLoading: infoLoading }">
            <Icon name="package-icon" size="48" />
            <div class="ml20 package-info-main flex-column flex-1">
                <span class="package-name flex-align-center">
                    {{ pkg.name }}
                    <span class="ml10 subtitle repo-tag" v-if="pkg.type === 'MAVEN'">
                        {{ pkg.key.replace(/^.*\/\/(.+):.*$/, '$1') }}
                    </span>
                </span>
                <span class="mt5 package-description">{{ pkg.description || '--' }}</span>
                <span class="mt5 package-create">{{ userList[pkg.lastModifiedBy] ? userList[pkg.lastModifiedBy].name : pkg.lastModifiedBy }} 更新于 {{ formatDate(pkg.lastModifiedDate) }}</span>
            </div>
            <bk-button class="pl5 pr5" theme="default" @click="$router.push({ name: 'searchPackageList' })">
                {{ $t('returnBack') }}
            </bk-button>
        </div>
        <div class="package-version flex-align-center">
            <div class="version-list" v-bkloading="{ isLoading }">
                <div class="version-search flex-align-center">
                    <bk-input v-model.trim="versionInput" clearable></bk-input>
                </div>
                <div class="version-list-main">
                    <div class="version-item text-overflow"
                        :class="{ 'selected': version.name === $route.query.version }"
                        v-for="version in filterVersionList"
                        :key="version.name"
                        :title="version.name"
                        @click="changeVersion(version)">
                        {{ version.name }}
                    </div>
                </div>
            </div>
            <version-detail class="version-detail" @refresh="getVersionListHandler"></version-detail>
        </div>
    </div>
</template>
<script>
    import versionDetail from './versionDetail'
    import { convertFileSize, formatDate } from '@/utils'
    import { mapState, mapActions } from 'vuex'
    export default {
        name: 'packageDetail',
        components: { versionDetail },
        data () {
            return {
                isLoading: false,
                infoLoading: false,
                pkg: {
                    name: '',
                    key: '',
                    downloads: 0,
                    versions: 0,
                    latest: '1.9',
                    lastModifiedBy: '',
                    lastModifiedDate: new Date()
                },
                versionInput: '',
                versionList: [],
                projectId: PROJECT_ID
            }
        },
        computed: {
            ...mapState(['userList']),
            repoType () {
                return this.$route.params.repoType
            },
            repoName () {
                return this.$route.params.repoName
            },
            packageKey () {
                return this.$route.query.package
            },
            filterVersionList () {
                return this.versionList.filter(version => ~version.name.indexOf(this.versionInput))
            }
        },
        created () {
            this.getPackageInfoHandler()
            this.getVersionListHandler()
        },
        methods: {
            formatDate,
            convertFileSize,
            ...mapActions([
                'getPackageInfo',
                'getVersionList'
            ]),
            getVersionListHandler () {
                this.isLoading = true
                this.getVersionList({
                    projectId: this.projectId,
                    repoName: this.repoName,
                    packageKey: this.packageKey,
                    current: 1,
                    limit: 1000
                }).then(({ records }) => {
                    this.versionList = records
                    this.changeVersion(records[0])
                }).finally(() => {
                    this.isLoading = false
                })
            },
            getPackageInfoHandler () {
                this.infoLoading = true
                this.getPackageInfo({
                    projectId: this.projectId,
                    repoName: this.repoName,
                    packageKey: this.packageKey
                }).then(info => {
                    this.pkg = info
                }).finally(() => {
                    this.infoLoading = false
                })
            },
            changeVersion (version) {
                this.$router.replace({
                    query: {
                        ...this.$route.query,
                        version: version.name
                    }
                })
            }
        }
    }
</script>
<style lang="scss" scoped>
@import '@/scss/conf';
.package-detail {
    .package-info {
        padding: 10px 20px 0;
        height: 80px;
        border-bottom: 1px solid $borderWeightColor;
        .package-info-main {
            overflow: hidden;
            .package-name {
                color: $fontBoldColor;
                font-size: 20px;
                font-weight: 500;
            }
            .package-description, .package-create {
                font-size: 12px;
                color: $fontColor;
            }
        }
    }
    .package-version {
        height: calc(100% - 80px);
        .version-list {
            height: 100%;
            flex-basis: 160px;
            overflow: hidden;
            border-right: 1px solid $borderWeightColor;
            .version-search {
                padding: 0 10px;
                height: 50px;
                border-bottom: 1px solid $borderWeightColor;
            }
            .version-list-main {
                height: calc(100% - 50px);
                overflow-y: auto;
                .version-item {
                    padding: 0 10px;
                    line-height: 32px;
                    cursor: pointer;
                    &.selected {
                        color: $primaryColor;
                        background-color: $primaryLightColor;
                    }
                }
            }
        }
        .version-detail {
            flex: 1;
            height: 100%;
            overflow: hidden;
        }
    }
}
</style>

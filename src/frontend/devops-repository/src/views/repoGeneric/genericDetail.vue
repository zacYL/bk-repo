<template>
    <bk-sideslider
        :is-show.sync="detailSlider.show"
        :title="detailSlider.data.name"
        @click.native.stop="() => {}"
        :quick-close="true"
        :width="800">
        <bk-tab class="mt10 ml20 mr20" slot="content" type="unborder-card" :active.sync="tabName">
            <bk-tab-panel name="detailInfo" :label="$t('baseInfo')">
                <div class="detail-info info-area" v-bkloading="{ isLoading: detailSlider.loading }">
                    <div class="flex-center" v-for="key in Object.keys(detailInfoMap)" :key="key">
                        <template v-if="detailSlider.data[key] && (key !== 'size' || !detailSlider.data.folder)">
                            <span>{{ detailInfoMap[key] }}</span>
                            <span class="pl40 break-all">{{ detailSlider.data[key] }}</span>
                        </template>
                    </div>
                </div>
                <div class="detail-info checksums-area" v-if="!detailSlider.folder" v-bkloading="{ isLoading: detailSlider.loading }">
                    <div class="flex-center" v-for="key of ['sha256', 'md5']" :key="key">
                        <span>{{ key.toUpperCase() }}</span>
                        <span class="pl40 break-all">{{ detailSlider.data[key] }}</span>
                    </div>
                </div>
            </bk-tab-panel>
            <bk-tab-panel v-if="!detailSlider.folder" name="metaDate" :label="$t('metaData')">
                <bk-table
                    class="mt20"
                    :data="Object.entries(detailSlider.data.metadata || {})"
                    stripe
                    :outer-border="false"
                    :row-border="false"
                    size="small"
                >
                    <div slot="prepend" class="pl15 add-metadata-main">
                        <bk-form form-type="inline" :label-width="80" :model="metadata" :rules="rules" ref="metadatForm">
                            <bk-form-item class="mr10" :required="true" property="key">
                                <bk-input style="width: 230px" size="small" v-model="metadata.key" :placeholder="$t('key')"></bk-input>
                            </bk-form-item>
                            <bk-form-item class="mr10" :required="true" property="value">
                                <bk-input style="width: 350px" size="small" v-model="metadata.value" :placeholder="$t('value')"></bk-input>
                            </bk-form-item>
                            <bk-form-item>
                                <i class="devops-icon icon-plus hover-btn" @click="addMetadataHandler"></i>
                            </bk-form-item>
                        </bk-form>
                    </div>
                    <bk-table-column :label="$t('key')" prop="0" width="250"></bk-table-column>
                    <bk-table-column :label="$t('value')" prop="1"></bk-table-column>
                    <bk-table-column :label="$t('operation')" width="97">
                        <template slot-scope="props">
                            <i class="devops-icon icon-delete hover-btn" @click="deleteMetadataHandler(props.row)"></i>
                        </template>
                    </bk-table-column>
                </bk-table>
            </bk-tab-panel>
        </bk-tab>
    </bk-sideslider>
</template>
<script>
    import { mapActions } from 'vuex'
    export default {
        name: 'genericDetail',
        props: {
            detailSlider: Object
        },
        data () {
            return {
                tabName: 'detailInfo',
                metadata: {
                    key: '',
                    value: ''
                },
                rules: {
                    key: [
                        {
                            required: true,
                            message: this.$t('pleaseInput') + this.$t('key'),
                            trigger: 'blur'
                        }
                    ],
                    value: [
                        {
                            required: true,
                            message: this.$t('pleaseInput') + this.$t('value'),
                            trigger: 'blur'
                        }
                    ]
                }
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            repoName () {
                return this.$route.query.name
            },
            detailInfoMap () {
                return {
                    'fullPath': this.$t('path'),
                    'size': this.$t('size'),
                    'createdBy': this.$t('createdBy'),
                    'createdDate': this.$t('createdDate'),
                    'lastModifiedBy': this.$t('lastModifiedBy'),
                    'lastModifiedDate': this.$t('lastModifiedDate')
                }
            }
        },
        methods: {
            ...mapActions(['addMetadata', 'deleteMetadata']),
            async addMetadataHandler () {
                await this.$refs.metadatForm.validate()
                this.addMetadata({
                    projectId: this.projectId,
                    repoName: this.repoName,
                    fullPath: this.detailSlider.data.fullPath,
                    body: {
                        metadata: {
                            [this.metadata.key]: this.metadata.value
                        }
                    }
                }).finally(() => {
                    this.metadata = {
                        key: '',
                        value: ''
                    }
                    this.$emit('refresh', true)
                })
            },
            deleteMetadataHandler (row) {
                this.deleteMetadata({
                    projectId: this.projectId,
                    repoName: this.repoName,
                    fullPath: this.detailSlider.data.fullPath,
                    body: {
                        keyList: [row[0]]
                    }
                }).finally(() => {
                    this.$emit('refresh', true)
                })
            }
        }
    }
</script>
<style lang="scss" scoped>
.detail-info {
    padding: 15px;
    margin-top: 40px;
    border: 1px solid var(--borderWeightColor);
    span {
        padding: 10px 0;
        flex: 4;
        &:first-child {
            flex: 1;
            display: flex;
            justify-content: flex-end;
        }
    }
    &.info-area:before {
        content: 'Info';
        position: absolute;
        padding: 0 10px;
        font-weight: 700;
        margin-top: -25px;
        background-color: white
    }
    &.checksums-area:before {
        content: 'Checksums';
        position: absolute;
        padding: 0 10px;
        font-weight: 700;
        margin-top: -25px;
        background-color: white
    }
}
.add-metadata-main {
    display: flex;
    align-items: center;
    height: 40px;
    border-bottom: 1px solid var(--borderWeightColor);
}
</style>

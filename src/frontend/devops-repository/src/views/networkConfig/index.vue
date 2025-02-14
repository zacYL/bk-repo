<template>
    <div class="repo-network-config">
        <div class="network-config-header">
            <div class="blue-tag"></div>
            <span>{{$t('networkSpeedLimitPolicy')}}</span>
        </div>
        <div class="network-config-main" v-bkloading="{ isLoading: initLoading }">
            <span class="tip-span">{{$t('networkSpeedLimitInfo')}}</span>
            <div class="config-main-input">
                <span style="margin-right: 20px;">{{$t('distributionRate')}}</span>
                <bk-input v-model="speedData" :min="0" :max="99999999" type="number" style="width: 180px;"></bk-input>
                <span class="ml10">MB/s</span>
            </div>
            <div class="config-main-bottom">
                <bk-button :disabled="!(Number(speedData) >= 0)" :loading="saveLoading" @click="clickSave" theme="primary">{{$t('save')}}</bk-button>
            </div>
        </div>
    </div>
</template>

<script>
    import { mapActions } from 'vuex'
    export default {
        data () {
            return {
                saveLoading: false,
                speedData: null,
                initLoading: false
            }
        },
        created () {
            this.init()
        },
        methods: {
            ...mapActions([
                'getNetworkRatelimit',
                'saveNetworkRatelimit'
            ]),
            // 获取参数
            init () {
                this.initLoading = true
                this.getNetworkRatelimit({ type: 'REPLICATION_NETWORK_RATE' }).then(res => {
                    const configuration = res?.configuration
                    this.speedData = Number.isNaN(Number(configuration)) ? null : Number(configuration)
                }).finally(() => {
                    this.initLoading = false
                })
            },
            // 保存参数
            clickSave () {
                this.saveLoading = true
                this.saveNetworkRatelimit({ type: 'REPLICATION_NETWORK_RATE', configuration: this.speedData ? Number(this.speedData) : -1 }).then(res => {
                    if (res) {
                        this.$bkMessage({
                            theme: 'success',
                            message: this.$t('save') + this.$t('space') + this.$t('success')
                        })
                    }
                }).finally(() => {
                    this.saveLoading = false
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    .repo-network-config {
        height: 100%;
        padding: 20px;
        background-color: white;

        .network-config-header {
            height: 24px;
            display: flex;
            align-items: center;
            font-size: 14px;
            font-weight: 600;
            color: #081E40;
            line-height: 24px;
            .blue-tag {
                height: 14px;
                width: 4px;
                border-radius: 2px;
                margin-right: 10px;
                flex-shrink: 0;
                background-color: #3A84FF;
            }
        }

        .network-config-main {
            margin-top: 10px;
            .tip-span {
                height: 20px;
                font-size: 12px;
                color: #8797AA;
            }
            .config-main-input {
                margin-top: 20px;
            }
            .config-main-bottom {
                margin-top: 40px;
            }
        }
    }
</style>

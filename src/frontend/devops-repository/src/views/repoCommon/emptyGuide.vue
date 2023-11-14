<template>
    <div class="empty-guide-container flex-center">
        <div class="" style="flex-wrap:wrap;">
            <img class="empty-guide-image" src="/ui/no-data.png" width="200" />
            <div class="flex-align-center">
                <div class="empty-guide-tip">{{$t('emptyGuide')}}</div>
                <bk-button class="ml20" @click="onClickUseGuide">
                    <span class="flex-align-center">
                        <Icon class="mr5" name="hand-guide" size="16" />
                        {{$t('guide')}}
                    </span>
                </bk-button>
            </div>
        </div>
        <useGuide ref="useGuideRef"></useGuide>
    </div>
</template>
<script>
    import useGuide from '@repository/views/repoCommon/useGuide'
    export default {
        name: 'emptyGuide',
        components: { useGuide },
        props: {
            article: {
                type: Array,
                default: () => []
            }
        },
        data () {
            return {
                ciMode: MODE_CONFIG === 'ci'
            }
        },
        computed: {
        },
        methods: {
            onClickUseGuide () {
                this.$refs.useGuideRef.setData({
                    show: true,
                    loading: false
                })
            }
        }
    }
</script>
<style lang="scss" scoped>
.empty-guide-container{
    flex-wrap:wrap;
    .empty-guide-tip {
        font-size: 26px;
        font-weight: bold;
        color: var(--fontPrimaryColor);
    }
    .empty-guide-image{
        display:block;
        margin:auto;
    }
}
</style>

<!--
 * @Author: xiaoshanwen
 * @Date: 2024-07-24 14:39:06
 * @LastEditTime: 2024-08-14 10:29:18
 * @Description:
 * @FilePath: /artifact/src/frontend/devops-repository/src/views/repoCommon/emptyGuide.vue
-->
<template>
    <div class="empty-guide-container flex-center">
        <div style="flex-wrap:wrap; margin-top:-120px;">
            <Icon class="empty-guide-common" name="empty-guide" size="200" />
            <span class="empty-guide-tip flex-center">{{$t('emptyGuide1')}}</span>
            <span class="mt5 empty-guide-tip2 flex-center">{{$t(storeType === 'local' ? 'emptyGuide2' : 'emptyGuide3')}}</span>
            <bk-button class="mt20 empty-guide-common" @click="onClickUseGuide">
                <span class="flex-align-center">
                    <Icon class="mr5" name="hand-guide" size="16" />
                    {{$t('guide')}}
                </span>
            </bk-button>
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
            storeType: {
                type: String,
                default: 'local'
            },
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
    .empty-guide-tip {
        font-size: 16px;
        font-weight: bold;
        color: var(--fontPrimaryColor);
    }
    .empty-guide-tip2 {
        font-size: 16px;
        font-weight: bold;
        color: var(--fontSubsidiaryColor);
    }
    .empty-guide-common{
        display:block;
        margin:auto;
    }
}
</style>

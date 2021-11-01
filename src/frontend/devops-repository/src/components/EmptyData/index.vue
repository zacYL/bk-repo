<template>
    <div class="empty-data-container flex-center" :style="exStyle">
        <template v-if="config">
            <div class="flex-column flex-center">
                <img :src="config.imgSrc" width="250" />
                <span class="mt5 empty-data-title">{{ config.title }}</span>
                <span class="mt5 empty-data-subtitle">{{ config.subTitle }}</span>
            </div>
        </template>
        <template v-else>
            <Icon :name="search ? 'empty-search' : 'empty-data'" size="30"></Icon>
            <slot>
                <span class="ml10 empty-tip">{{ search ? $t('noSearchData') : $t('noData') }}</span>
            </slot>
        </template>
    </div>
</template>
<script>
    export default {
        name: 'emptyData',
        props: {
            search: {
                type: Boolean,
                default: false
            },
            exStyle: {
                type: String,
                default: ''
            },
            config: {
                type: Object
            }
        }
    }
</script>
<style lang="scss" scoped>
.empty-data-container {
    .empty-data-title {
        font-size: 14px;
        font-weight: bold;
    }
    .empty-data-subtitle {
        color: var(--fontSubsidiaryColor);
    }
}
</style>

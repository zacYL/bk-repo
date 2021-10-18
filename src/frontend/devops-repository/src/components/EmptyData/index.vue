<template>
    <div class="empty-data-container flex-center" :style="exStyle">
        <Icon :name="search ? 'empty-search' : 'empty-data'" size="30"></Icon>
        <slot>
            <span class="ml10 empty-tip">{{ search ? $t('noSearchData') : $t('noData') }}</span>
        </slot>
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
            }
        }
    }
</script>

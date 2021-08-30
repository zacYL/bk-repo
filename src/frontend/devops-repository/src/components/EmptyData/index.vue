<template>
    <div class="empty-data-container flex-center" :style="exStyle">
        <Icon :name="iconName" size="36"></Icon>
        <slot>
            <span class="ml10 empty-tip">{{ $t('noData') }}</span>
        </slot>
    </div>
</template>
<script>
    export default {
        name: 'emptyData',
        props: {
            iconName: {
                type: String,
                default: 'empty-data'
            },
            exStyle: {
                type: String,
                default: ''
            }
        }
    }
</script>

<template>
    <bk-dialog
        :value="value"
        :width="width"
        :position="position"
        :close-icon="false">
        <template #tools>
            <div class="mb20 canway-dialog-header flex-align-center">
                <span class="canway-dialog-title">{{ title }}</span>
                <i class="devops-icon icon-close" @click="$emit('cancel')"></i>
            </div>
        </template>
        <slot></slot>
        <template #footer>
            <slot name="footer">
                <bk-button @click="$emit('cancel')">{{$t('cancel')}}</bk-button>
                <bk-button class="ml10" theme="primary" @click="$emit('confirm')">{{$t('confirm')}}</bk-button>
            </slot>
        </template>
    </bk-dialog>
</template>
<script>
    export default {
        name: 'canwayDialog',
        props: {
            value: Boolean,
            title: String,
            width: [Number, String],
            position: Object
        }
    }
</script>
<style lang="scss" scoped>
.canway-dialog-header {
    height: 50px;
    padding: 0 20px;
    justify-content: space-between;
    color: #222;
    border-bottom: 1px solid #EBEDF0;
    cursor: default;
    .canway-dialog-title {
        font-size: 16px;
        font-weight: bold;
    }
    .icon-close {
        padding: 5px;
        font-size: 12px;
        cursor: pointer;
    }
}
</style>

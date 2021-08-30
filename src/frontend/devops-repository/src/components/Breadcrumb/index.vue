<template>
    <bk-breadcrumb class="breadcrumb" separator-class="bk-icon icon-angle-right">
        <slot></slot>
        <bk-breadcrumb-item
            class="breadcrumb-item"
            v-for="item in list"
            :key="item.name">
            <span
                class="breadcrumb-label"
                @click="() => item.cilckHandler && item.cilckHandler(item)">
                {{ item.name }}
            </span>
        </bk-breadcrumb-item>
    </bk-breadcrumb>
</template>
<script>
    export default {
        name: 'breadCrumb',
        props: {
            list: {
                type: Array,
                default: []
            }
        }
    }
</script>
<style lang="scss" scoped>
.breadcrumb {
    color: #63656e;
    .breadcrumb-item {
        &:last-child {
            color: #979ba5;
        }
        .breadcrumb-label {
            cursor: pointer;
        }
    }
}
</style>

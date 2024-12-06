<!--
 * @Date: 2024-12-05 15:00:22
 * @LastEditors: xiaoshan
 * @LastEditTime: 2024-12-06 09:52:59
 * @FilePath: /artifact/src/frontend/devops-repository/src/components/OperationList/index.vue
-->
<template>
    <bk-popover
        :class="{ 'operation-trigger': !Object.keys($slots).length }"
        placement="bottom-end"
        theme="light"
        ext-cls="operation-container"
        :on-show="() => $emit('show')"
        :tippy-options="{ trigger: 'click' }"
        v-bind="$attrs">
        <slot>
            <i class="devops-icon icon-more flex-center hover-btn"></i>
        </slot>
        <template #content><ul class="operation-list">
            <li v-for="li in filterList" :key="li.label"
                class="operation-item"
                :class="{ 'disabled': li.disabled }"
                @click.stop="() => !li.disabled && li.clickEvent()">
                {{ li.label }}
            </li>
        </ul></template>
    </bk-popover>
</template>
<script>
    export default {
        name: 'operationList',
        props: {
            list: {
                type: Array,
                default: () => []
            }
        },
        computed: {
            filterList () {
                return this.list.filter(Boolean)
            }
        }
    }
</script>
<style lang="scss">
.operation-trigger {
    border-radius: 2px;
    cursor: pointer;
    .icon-more {
        padding: 3px;
        font-size: 18px;
    }
}
</style>

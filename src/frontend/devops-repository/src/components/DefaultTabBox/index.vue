<!--
 * @Date: 2024-11-08 18:36:10
 * @LastEditors: xiaoshan
 * @LastEditTime: 2024-11-08 18:36:28
 * @FilePath: /artifact/src/frontend/devops-repository/src/components/DefaultTabBox/index.vue
-->

<template>
    <div class="nav-box" :class="[theme === 'primary' ? 'primary-theme-box' : '']">
        <div
            v-for="tab in tabs"
            :key="tab[idKey]"
            :class="['tab', theme === 'primary' ? 'theme-primary' : '', currentTab[idKey] === tab[idKey] ? 'is-active' : '', checkAble(tab) ? '' : 'is-disable', !isSlot ? 'tab-padding' : '']"
            @click="checkAble(tab) && eventHandle(tab) && $emit('tabChang', tab)">
            <slot :data="tab">
            </slot>
            <template v-if="!isSlot">
                {{tab[labelKey]}}
            </template>
        </div>
    </div>
</template>
<script>
    export default {
        name: 'default-tab-box',
        props: {
            theme: {
                type: String,
                default: ''
            },
            tabs: {
                type: Array,
                default: () => {
                    return []
                }
            },
            idKey: {
                type: String,
                default: 'id'
            },
            labelKey: {
                type: String,
                default: 'label'
            },
            checkFnName: {
                type: String,
                default: 'check'
            },
            currentTab: {
                type: Object,
                default: () => {
                    return {}
                }
            },
            isSlot: {
                type: Boolean,
                default: false
            }
        },
        methods: {
            eventHandle (tab) {
                try {
                    (tab.event && tab.event())
                } catch (error) {
                    console.error(error)
                }
                return true
            },
            checkAble (tab) {
                return (this.checkFnName && tab[this.checkFnName]) ? tab[this.checkFnName](tab) : true
            }
        }
    }
</script>

<style lang="scss" scoped>
@import './style.scss';
</style>

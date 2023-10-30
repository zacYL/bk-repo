<template>
    <bk-breadcrumb separator-class="bk-icon icon-angle-right">
        <slot></slot>
        <bk-breadcrumb-item
            v-for="item in list"
            :key="item.name"
            :to="{ name: item.name, params: { ...$route.query, ...$route.params }, query: $route.query }">
            {{ transformLabel(item.label) || $t(item.template) }}
        </bk-breadcrumb-item>
    </bk-breadcrumb>
</template>
<script>
    export default {
        name: 'topBreadCrumb',
        computed: {
            list () {
                return this.$route.matched.map(r => r.meta.breadcrumb || []).flat(Infinity)
            }
        },
        methods: {
            transformLabel (label) {
                const ctx = { ...this.$route.params, ...this.$route.query }
                const regexp = /\{(.*?)\}/g
                // 当label不是{开头}结尾时，表示此时是一级菜单或是静态值，直接国际化即可
                const transformLabel = regexp.test(label)
                    ? label.replace(regexp, (_, $1) => {
                        return $1 in ctx ? ctx[$1] : ''
                    })
                    : this.$t(label)
                return this.replaceRepoName(transformLabel)
            }
        }
    }
</script>

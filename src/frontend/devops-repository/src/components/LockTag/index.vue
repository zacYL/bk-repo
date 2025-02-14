<template>
    <Icon class="table-svg mr5 tag-hover" size="20" name="lock" v-bk-tooltips="{ content: tooltipContent, placements: ['top'],disabled: !tooltipContent }" />
</template>
<script>
    import { mapState } from 'vuex'
    export default {
        name: 'lockTag',
        props: {
            lockUser: String,
            // lockType: String,
            lockDescription: String
        },
        computed: {
            ...mapState(['userList']),
            tooltipContent () {
                return `${(this.userList[this.lockUser]?.name || this.lockUser) || this.lockUser} ${this.$t('lock')} ${this.lockDescription ? this.$t('limitTagReason') + this.lockDescription : ''}`
            }
        }
    }
</script>
<style lang="scss" scoped>
.tag-hover{
    cursor: pointer;
    vertical-align: top;
}
</style>

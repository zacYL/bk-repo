<template>
    <span class="repo-tag STOP" :data-name="$t('forbid')"
        v-bk-tooltips=" { content: tooltipContent, placements: ['top'] ,disabled: !tooltipContent } "
    ></span>
</template>
<script>
    import { mapState } from 'vuex'
    export default {
        name: 'forbidTag',
        props: {
            forbidUser: String,
            forbidType: String,
            forbidDescription: String
        },
        computed: {
            ...mapState(['userList']),
            tooltipContent () {
                switch (this.forbidType) {
                    case 'SCANNING':
                        return this.$t('forbidTip1')
                    case 'QUALITY_UNPASS':
                        return this.$t('forbidTip2')
                    case 'MANUAL':
                        return `${(this.userList[this.forbidUser]?.name || this.forbidUser) || this.forbidUser} ${this.$t('manualBan')} ${this.forbidDescription ? this.$t('limitTagReason') + this.forbidDescription : ''}`
                    default:
                        return ''
                }
            }
        }
    }
</script>

<template>
    <div class="rule-item flex-align-center">
        <span class="mr5">制品元数据满足</span>
        <bk-input
            style="width:180px;"
            :value="rule.metadataName"
            @input="changeInput"
            :disabled="disabled"
            placeholder="属性键">
        </bk-input>
        <select-input
            :select="rule.operationType"
            :select-list="typeList"
            :input="rule.metadataValue"
            :disabled="disabled"
            placeholder="属性值"
            @change="r => change(r)">
        </select-input>
        <Icon v-show="!disabled" class="ml10 hover-btn" size="24" name="icon-delete" @click.native="$emit('delete')" />
    </div>
</template>
<script>
    import SelectInput from '@repository/components/SelectInput'
    export default {
        name: 'metadataRule',
        components: { SelectInput },
        props: {
            disabled: Boolean,
            rule: {
                type: Object,
                default: () => ({
                    metadataName: '',
                    metadataValue: '',
                    operationType: 'EQ'
                })
            }
        },
        data () {
            return {
                typeList: [
                    { id: 'EQ', name: '等于' },
                    { id: 'IN', name: '包含' },
                    { id: 'REGEX', name: '正则匹配' }
                ]
            }
        },
        methods: {
            changeInput (metadataName) {
                this.change({
                    metadataName
                })
            },
            change ({
                metadataName = this.rule.metadataName,
                select: operationType = this.rule.operationType,
                input: metadataValue = this.rule.metadataValue
            }) {
                this.$emit('change', {
                    operationType,
                    metadataName,
                    metadataValue
                })
            }
        }
    }
</script>
<style lang="scss" scoped>
.rule-item {
    &:not(:nth-child(1)):before {
        content: '或者';
        position: absolute;
        margin-left: -30px;
    }
}
</style>

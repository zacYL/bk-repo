<template>
    <!-- 制品类型下拉筛选项 -->
    <div class="type-select-container flex-center"
        :class="{ 'active': showDropdown }"
        @click="showDropdown = !showDropdown"
        v-bk-clickoutside="hiddenDropdown">
        <div class="flex-align-center">
            <Icon size="20" :name="repoType" />
            <span class="ml5">{{repoType}}</span>
        </div>
        <i class="ml5 devops-icon" :class="showDropdown ? 'icon-angle-up' : 'icon-angle-down'"></i>
        <div v-show="showDropdown" class="dropdown-list" @click.stop="() => {}">
            <bk-radio-group :value="repoType" class="repo-type-radio-group" @change="changeType">
                <bk-radio-button v-for="repo in repoList" :key="repo" :value="repo">
                    <div class="flex-align-center repo-type-radio" :class="{ 'checked': repo === repoType }">
                        <Icon size="20" :name="repo" />
                        <span class="ml10">{{repo}}</span>
                    </div>
                </bk-radio-button>
            </bk-radio-group>
        </div>
    </div>
</template>
<script>
    export default {
        name: 'typeSelect',
        props: {
            repoList: {
                type: Array,
                default: () => []
            },
            repoType: {
                type: String,
                default: 'generic'
            }
        },
        data () {
            return {
                showDropdown: false
            }
        },
        methods: {
            hiddenDropdown () {
                this.showDropdown = false
            },
            changeType (type) {
                this.$emit('change', type)
                this.hiddenDropdown()
            }
        }
    }
</script>
<style lang="scss" scoped>
.type-select-container {
    position: relative;
    width: 120px;
    height: 48px;
    margin-right: -1px;
    border-radius: 2px 0 0 2px;
    border: 1px solid var(--borderWeightColor);
    cursor: pointer;
    &.active {
        color: var(--primaryColor);
        border-color: var(--primaryColor);
        z-index: 1;
    }
    .icon-angle-up,
    .icon-angle-down {
        font-size: 12px;
        font-weight: bold;
        color: var(--fontSubsidiaryColor);
        transform: scale(0.8)
    }
    .dropdown-list {
        position: absolute;
        top: calc(100% + 10px);
        left: 0;
        width: 120px;
        max-height: 216px;
        overflow-y: auto;
        padding: 5px 10px;
        background-color: white;
        box-shadow: 0px 0px 6px 0px rgba(167, 167, 167, 0.5);
        z-index: 1;
        cursor: default;
        .repo-type-radio-group {
            display: grid;
            grid-template: auto / repeat(1, 50px);
            gap: 6px;
            justify-items: start;
            ::v-deep .bk-form-radio-button {
                .bk-radio-button-text {
                    height: 100%;
                    line-height: initial;
                    padding: 0;
                    border: none;
                }
            }
            .repo-type-radio {
                position: relative;
                padding: 0 0 0 5px;
                width: 100px;
                height: 30px;
                &.checked {
                    height: 30px;
                    background-color: var(--bgHoverLighterColor);
                    color: var(--primaryColor) ;
                }
            }
        }
    }
}
</style>

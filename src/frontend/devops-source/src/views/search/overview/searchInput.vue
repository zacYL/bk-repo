<template>
    <div class="repo-search-component">
        <div class="search-type flex-align-center">
            <span
                class="repo-type-radio flex-center"
                :class="{
                    'selected': search.type === type
                }"
                v-for="type in repoEnum"
                :key="type"
                @click="search.type = type">
                <Icon size="20" :name="type" />
            </span>
        </div>
        <div class="flex-align-center">
            <bk-input
                class="search-input"
                v-model.trim="search.name"
                size="large"
                :placeholder="$t('pleaseInput') + $t('packageName')"
                clearable>
            </bk-input>
            <bk-button size="large" theme="primary" @click="$emit('search', search)">
                <i class="devops-icon icon-search"></i>
                {{$t('search')}}
            </bk-button>
        </div>
    </div>
</template>
<script>
    import { repoEnum } from '@/store/publicEnum'
    export default {
        name: 'searchInput',
        data () {
            return {
                repoEnum,
                search: {
                    name: '',
                    type: this.$route.query.repoType || repoEnum[0]
                }
            }
        }
    }
</script>
<style lang="scss" scoped>
@import '@/scss/conf';
.repo-search-component {
    width: 560px;
    .search-type {
        position: relative;
        z-index: 1;
        margin-bottom: -1px;
        .repo-type-radio {
            width: 42px;
            height: 36px;
            cursor: pointer;
            border: 1px solid transparent;
            &.selected {
                border-color: $primaryColor $primaryColor white $primaryColor;
                border-radius: 2px 2px 0px 0px;
            }
        }
    }
    .search-input {
        flex: 1;
        margin-right: -1px;
        ::v-deep .bk-form-input {
            border-color: #3a84ff;
            border-radius: 0 0 0 2px;
            border-right-color: transparent;
        }
    }
}
</style>

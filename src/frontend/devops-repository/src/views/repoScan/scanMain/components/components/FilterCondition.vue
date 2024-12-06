<template>
    <div style="display: flex;">
        <bk-popover ref="recyclingPopover"
            :tippy-options="{
                theme: 'light',
                trigger: 'click',
                hideOnClick: false
            }"
            ext-cls="recycling-popover"
            v-bk-clickoutside="clickoutside">
            <bk-button v-bind="withParams ? { theme: 'primary',outline: true } : { theme: 'default' }">
                <div
                    class="flex-align-center"
                >
                    <span>{{ $t('filter') }}</span>
                </div>
            </bk-button>
            <div slot="content">
                <main>
                    <bk-form :model="query" :label-width="110" form-type="vertical" :rules="rules" ref="queryForm">
                        <!-- 制品类型 -->
                        <bk-form-item :label="$t('repoType')" :property="'packageType'">
                            <bk-select
                                v-model="query.packageType"
                                :clearable="false"
                                :placeholder="$t('allTypes')">
                                <bk-option v-for="type in repoEnum.filter(v => v.value !== 'generic')" :key="type.value" :id="type.value" :name="type.label">
                                    <div class="flex-align-center">
                                        <Icon size="20" :name="type.value" />
                                        <span class="ml10 flex-1 text-overflow">{{type.label}}</span>
                                    </div>
                                </bk-option>
                            </bk-select>
                        </bk-form-item>
                        <!-- 制品名称 -->
                        <bk-form-item :label="$t('artifactName')" :property="'key'">
                            <bk-input
                                v-model="query.key"
                                :placeholder="$t('artifactNamePlaceholder')"
                                max-length="100"
                                clearable
                            />
                        </bk-form-item>
                        <!-- 版本号 -->
                        <bk-form-item :label="$t('searchConditionVersion')" :property="'version'">
                            <bk-input
                                v-model="query.version"
                                :placeholder="$t('enterVersionNumber')"
                                max-length="100"
                                clearable
                            />
                        </bk-form-item>
                    </bk-form>
                </main>
                <footer class="flex-align-center"
                    style="justify-content: flex-end;">
                    <bk-button @click="reset" class="mr10">
                        {{$t('reset')}}
                    </bk-button>
                    <bk-button theme="primary" @click="confirm">
                        {{$t('search')}}
                    </bk-button>
                </footer>
            </div>
        </bk-popover>
    </div>
</template>
<script>

    import { repoEnum } from '@repository/store/publicEnum'
    import { cloneDeep } from 'lodash'
    export default {
        name: 'filter-condition',
        props: {
            withParams: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                repoEnum, // 制品类型
                query: {
                    packageType: 'maven',
                    key: '', // 制品名称
                    version: ''
                },
                rules: {
                }
            }
        },
        methods: {
            // 处理popover点击外层是否关闭的逻辑
            clickoutside (e) {
                const tippy = document.querySelectorAll('.tippy-popper')
                const allTippy = [...tippy]
                let isInTippy = false
                allTippy.forEach(item => {
                    if (item.contains(e.target)) {
                        isInTippy = true
                    }
                })
                if (!isInTippy) {
                    this.$refs.recyclingPopover.hideHandler()
                }
            },

            // 重置
            clear () {
                Object.assign(this.query, {
                    packageType: 'maven',
                    key: '', // 制品名称
                    version: ''
                })
            },
            reset () {
                this.clear()
                this.$emit('reset', () => {
                    this.close()
                })
            },
            close () {
                this.$refs.recyclingPopover.hideHandler()
            },
            confirm () {
                this.$refs.queryForm.validate().then(() => {
                    this.$emit('confirm', cloneDeep(this.query), () => {
                        this.close()
                    })
                }).catch(() => {})
            }
        }
    }
</script>
<style lang="scss">
.recycling-popover {
    .tippy-tooltip {
        padding: 0;
    }
}
.select-popover-custom{
    width: 150px !important;
}
</style>
<style lang="scss" scoped>
main {
    width: 400px;
    padding: 20px 25px;
}
footer {
    border-top: 1px solid #ECF2FB;
    height: 42px;
    padding: 0 20px;
}
</style>

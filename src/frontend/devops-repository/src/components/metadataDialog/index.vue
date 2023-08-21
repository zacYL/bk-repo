<template>
    <div class="metadata-add" v-bk-clickoutside="hiddenAddMetadata">
        <div @click="metadata.show ? hiddenAddMetadata() : showAddMetadata()">
            <!-- 支持通过插槽修改 -->
            <slot name="trigger">
                <i class="devops-icon icon-plus flex-center hover-btn"></i>
            </slot>
        </div>
        <div class="metadata-add-board"
            :style="{ height: metadata.show ? '230px' : '0' }">
            <bk-form class="p20" :label-width="80" :model="metadata" :rules="rules" ref="metadatForm">
                <bk-form-item :label="$t('key')" :required="true" property="key">
                    <bk-input size="small" v-model.trim="metadata.key" :placeholder="$t('key')"></bk-input>
                </bk-form-item>
                <bk-form-item :label="$t('value')" :required="true" property="value">
                    <bk-input size="small" v-model.trim="metadata.value" :placeholder="$t('value')"></bk-input>
                </bk-form-item>
                <bk-form-item :label="$t('description')">
                    <bk-input size="small" v-model.trim="metadata.description" :placeholder="$t('description')"></bk-input>
                </bk-form-item>
                <bk-form-item>
                    <bk-button size="small" theme="default" @click.stop="hiddenAddMetadata">{{$t('cancel')}}</bk-button>
                    <bk-button class="ml5" size="small" :loading="metadata.loading" theme="primary" @click="addMetadata">{{$t('confirm')}}</bk-button>
                </bk-form-item>
            </bk-form>
        </div>
    </div>
</template>

<script>
    export default {
        name: 'metadataDialog',
        props: {
        },
        data () {
            return {
                metadata: {
                    show: false,
                    loading: false,
                    key: '',
                    value: '',
                    description: ''
                },
                rules: {
                    key: [
                        {
                            required: true,
                            message: this.$t('pleaseInput') + this.$t('key'),
                            trigger: 'blur'
                        },
                        {
                            min: 1,
                            max: 30,
                            message: this.$t('metadataNoLegalKeyInfo'),
                            trigger: 'blur'
                        }
                    ],
                    value: [
                        {
                            required: true,
                            message: this.$t('pleaseInput') + this.$t('value'),
                            trigger: 'blur'
                        },
                        {
                            min: 1,
                            max: 500,
                            message: this.$t('metadataNoLegalValueInfo'),
                            trigger: 'blur'
                        }
                    ]
                }
            }
        },
        methods: {
            showAddMetadata () {
                this.metadata = {
                    show: true,
                    loading: false,
                    key: '',
                    value: '',
                    description: ''
                }
            },
            hiddenAddMetadata () {
                this.metadata.show = false
                this.$refs.metadatForm.clearError()
            },
            async addMetadata () {
                await this.$refs.metadatForm.validate()
                this.metadata.loading = true
                this.$emit('add-metadata', this.metadata)
                this.metadata.loading = false
            }

        }
    }
</script>

<style lang="scss" scoped>
 .metadata-add {
    position: absolute;
    display: flex;
    align-items: center;
    justify-content: center;
    top: 0;
    right: 25px;
    width: 35px;
    height: 40px;
    z-index: 1;
    .metadata-add-board {
        position: absolute;
        top: 42px;
        right: -25px;
        width: 300px;
        overflow: hidden;
        background: white;
        border-radius: 2px;
        box-shadow: 0 3px 6px rgba(51, 60, 72, 0.4);
        will-change: height;
        transition: all .3s;
    }
}
</style>

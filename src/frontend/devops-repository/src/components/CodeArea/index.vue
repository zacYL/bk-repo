<template>
    <div class="code-area"
        :style="{
            'background-color': bgColor,
            'color': color
        }"
        @click="copyCode()">
        <div v-for="code in codeList" :key="code + Math.random()"
            :class="{
                'code-main': true,
                'line-number': showLineNumber && codeList.length > 1
            }">
            <pre class="code-pre">{{ code }}</pre>
        </div>
        <i class="code-copy devops-icon icon-clipboard hover-btn"></i>
    </div>
</template>
<script>
    import Clipboard from 'clipboard'
    export default {
        name: 'codeArea',
        props: {
            codeList: {
                type: Array,
                default: () => []
            },
            showLineNumber: {
                type: Boolean,
                default: true
            },
            bgColor: {
                type: String,
                default: '#EBF3FF'
            },
            color: {
                type: String,
                default: '#081E40'
            }
        },
        methods: {
            copyCode () {
                // eslint-disable-next-line prefer-const
                const clipboard = new Clipboard('.code-area', {
                    text: () => {
                        return this.codeList.join('\n')
                    }
                })
                clipboard.on('success', (e) => {
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('copy') + this.$t('success')
                    })
                    clipboard.destroy()
                })
                clipboard.on('error', (e) => {
                    this.$bkMessage({
                        theme: 'error',
                        message: this.$t('copy') + this.$t('fail')
                    })
                    clipboard.destroy()
                })
            }
        }
    }
</script>
<style lang="scss" scoped>
.code-area {
    position: relative;
    line-height: 2;
    padding: 10px 35px;
    min-height: 48px;
    word-break: break-all;
    counter-reset: row-num;
    .code-main {
        position: relative;
        &.line-number:before {
            position: absolute;
            margin-left: -25px;
            counter-increment: row-num;
            content: counter(row-num);
        }
        .code-pre {
            font-family: Helvetica Neue,Arial,PingFang SC,Hiragino Sans GB,Microsoft Yahei,WenQuanYi Micro Hei,sans-serif;
            white-space: pre-wrap;
            margin: 0;
        }
    }
    .code-copy {
        position: absolute;
        visibility: hidden;
        top: 10px;
        right: 10px;
        font-size: 24px;
    }
    &:hover .code-copy {
        visibility: visible;
    }
}
</style>

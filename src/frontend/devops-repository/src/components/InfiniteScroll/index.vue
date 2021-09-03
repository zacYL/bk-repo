<template>
    <div class="infinite-scroll-container">
        <div class="infinite-scroll-list" :class="{ 'need-scroll': needScroll }">
            <slot></slot>
            <div v-if="!hasNext" class="loading-more">没有更多了</div>
            <div v-else class="loading-more"><i class="devops-icon icon-circle-2-1 spin-icon"></i>正在加载中</div>
        </div>
    </div>
</template>

<script>
    import { throttle } from '@/utils'
    const SCROLL_THRESHOLD = 100
    export default {
        name: 'InfiniteScroll',
        props: {
            isLoading: Boolean,
            hasNext: Boolean
        },
        data () {
            return {
                needScroll: false
            }
        },
        watch: {
            isLoading (val) {
                // 内容加载完成
                if (!val) {
                    const target = this.$el.querySelector('.infinite-scroll-list')
                    if (target.scrollHeight > target.offsetHeight) this.needScroll = true
                    else this.needScroll = false
                }
            }
        },
        mounted () {
            this.$el.querySelector('.infinite-scroll-list').addEventListener('scroll', throttle(this.handleScroll, 500))
        },
        methods: {
            handleScroll () {
                const { hasNext, isLoading } = this
                if (!hasNext || isLoading) return
                const target = this.$el.querySelector('.infinite-scroll-list')
                const offset = target.scrollHeight - (target.offsetHeight + target.scrollTop)
                if (offset <= SCROLL_THRESHOLD) { // scroll to end
                    this.$emit('load')
                }
            },
            scrollToTop () {
                const target = this.$el.querySelector('.infinite-scroll-list')
                target.scrollTo(0, 0)
            }
        }
    }
</script>

<style lang="scss" scoped>
.infinite-scroll-container {
    height: 100%;
    .infinite-scroll-list {
        height: 100%;
        overflow-y: auto;
        &.need-scroll {
            margin-right: -12px;
        }
        &::-webkit-scrollbar {
            width: 12px;
            background-color: var(--bgLightColor);
        }
        &::-webkit-scrollbar-thumb {
            border-radius: initial;
        }
        .loading-more {
            display: flex;
            justify-content: center;
            align-items: center;
            .devops-icon {
                margin-right: 8px;
            }
        }
    }
}
</style>

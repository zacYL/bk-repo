<template>
    <div class="infinite-scroll-container">
        <slot></slot>
        <div v-if="!hasNext" class="loading-more">没有更多了</div>
        <div v-else class="loading-more"><i class="devops-icon icon-circle-2-1 spin-icon"></i>正在加载中</div>
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
        mounted () {
            this.$el.addEventListener('scroll', throttle(this.handleScroll, 500))
        },
        methods: {
            handleScroll () {
                const { hasNext, isLoading } = this
                if (!hasNext || isLoading) return
                const target = this.$el
                const offset = target.scrollHeight - (target.offsetHeight + target.scrollTop)
                if (offset <= SCROLL_THRESHOLD) { // scroll to end
                    this.$emit('load')
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
.infinite-scroll-container {
    height: 100%;
    overflow-y: auto;
    &::-webkit-scrollbar {
        width: 0;
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
</style>

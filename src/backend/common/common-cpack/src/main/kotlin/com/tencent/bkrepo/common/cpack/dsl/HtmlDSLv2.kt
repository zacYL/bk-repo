package com.tencent.bkrepo.common.cpack.dsl

/**
 * 建议HTML 渲染工具
 */
interface Node {
    fun render(step: Int = 0): String
}

class StringNode(private val content: String) : Node {
    override fun render(step: Int): String {
        val stringBuilder = StringBuilder("")
        for (i in 1 until step) {
            stringBuilder.append("    ")
        }
        val space = stringBuilder.toString()
        return "$space$content"
    }
}

class NestNode(private val content: String) : Node {
    override fun render(step: Int): String {
        return content
    }
}

class BlockNode(private val name: String) : Node {
    // 子节点
    val childrenNode = mutableListOf<Node>()
    // 元素属性
    private val properties = mutableMapOf<String, Any>()

    // 该属性决定元素是否换行
    private val lineFeed: String = "lineFeed"
    private val external = arrayOf(lineFeed)

    // 下面主要是为了对html 中一些特殊元素做额外处理
    // 无需换行的元素
    private val unLineFeed = arrayOf("a")
    // 自闭合元素
    private val selfClose = arrayOf("meta", "br", "hr")
    // 预定义格式元素
    private val preformatted = arrayOf("pre")
    // 根元素
    private val protocol = arrayOf("html")

    // 核心渲染方法，拼接元素标签、属性、文本内容、子节点
    private fun renderComplex(step: Int, space: String): String {
        // 根元素添加声明头<!DOCTYPE html>
        val stringBuilder = if (name in protocol) {
            StringBuilder("<!DOCTYPE html>\n$space<$name")
        } else {
            StringBuilder("$space<$name")
        }
        // 过滤非html定义的属性
        val htmlProperties = properties.filter { it.key != lineFeed }
        if (htmlProperties.isNotEmpty()) {
            val propertyString = htmlProperties.filter { it.key !in external }
                .map { "${it.key}='${it.value}'" }.joinToString(" ")
            stringBuilder.append(" $propertyString")
        }
        // 自闭合元素添加结束标签后返回
        if (name in selfClose) {
            stringBuilder.append(">")
            return stringBuilder.toString()
        }
        stringBuilder.append(">")
        // 判断是否换行
        val nodeLineFeed = properties.filterKeys { it == lineFeed }.map { it.value }.firstOrNull() as? Boolean
        var nextStep = 0
        // 对闭合标签的换行及预定义格式元素缩进做判断
        if (nodeLineFeed != false && name !in unLineFeed) {
            stringBuilder.append("\n")
            //
            if (childrenNode.isNotEmpty() && name !in preformatted) {
                nextStep = step.inc()
            }
            stringBuilder.append(childrenNode.joinToString("\n") { it.render(nextStep) })
            stringBuilder.append("\n$space")
        } else {
            stringBuilder.append(childrenNode.joinToString("\n") { it.render(nextStep) })
        }
        stringBuilder.append("</$name>")
        return stringBuilder.toString()
    }

    override fun render(step: Int): String {
        // 缩进渲染
        val stringBuilder = StringBuilder("")
        for (i in 1 until step) {
            stringBuilder.append("    ")
        }
        val space = stringBuilder.toString()
        return renderComplex(step, space)
    }

    operator fun String.invoke(block: BlockNode.() -> Unit): BlockNode {
        val node = BlockNode(this)
        node.block()
        this@BlockNode.childrenNode += node
        return node
    }

    operator fun String.invoke(value: Any) {
        this@BlockNode.properties[this] = value
    }

    // 操作附重载  `+`连接html元素与文本内容
    operator fun String.unaryPlus() {
        this@BlockNode.childrenNode += StringNode(this)
    }

    // 操作附重载  `+`连接嵌套 渲染完成html内容
    operator fun String.unaryMinus() {
        this@BlockNode.childrenNode += NestNode(this)
    }
}

fun html(block: BlockNode.() -> Unit): BlockNode {
    val html = BlockNode("html")
    html.block()
    return html
}

fun table(block: BlockNode.() -> Unit): BlockNode {
    val table = BlockNode("table")
    table.block()
    return table
}

fun tr(block: BlockNode.() -> Unit): BlockNode {
    val table = BlockNode("tr")
    table.block()
    return table
}

fun a(block: BlockNode.() -> Unit): BlockNode {
    val table = BlockNode("a")
    table.block()
    return table
}

fun BlockNode.head(block: BlockNode.() -> Unit): BlockNode {
    val head = BlockNode("head")
    head.block()
    this.childrenNode += head
    return head
}

fun BlockNode.title(block: BlockNode.() -> Unit): BlockNode {
    val head = BlockNode("title")
    head.block()
    this.childrenNode += head
    return head
}

fun BlockNode.h1(block: BlockNode.() -> Unit): BlockNode {
    val head = BlockNode("h1")
    head.block()
    this.childrenNode += head
    return head
}

fun BlockNode.hr(block: BlockNode.() -> Unit): BlockNode {
    val head = BlockNode("hr")
    head.block()
    this.childrenNode += head
    return head
}

fun BlockNode.pre(block: BlockNode.() -> Unit): BlockNode {
    val head = BlockNode("pre")
    head.block()
    this.childrenNode += head
    return head
}

fun BlockNode.meta(block: BlockNode.() -> Unit): BlockNode {
    val head = BlockNode("meta")
    head.block()
    this.childrenNode += head
    return head
}

fun BlockNode.address(block: BlockNode.() -> Unit): BlockNode {
    val head = BlockNode("address")
    head.block()
    this.childrenNode += head
    return head
}

fun BlockNode.body(block: BlockNode.() -> Unit): BlockNode {
    val body = BlockNode("body")
    body.block()
    this.childrenNode += body
    return body
}

fun BlockNode.table(block: BlockNode.() -> Unit): BlockNode {
    val body = BlockNode("table")
    body.block()
    this.childrenNode += body
    return body
}

fun BlockNode.tbody(block: BlockNode.() -> Unit): BlockNode {
    val body = BlockNode("tbody")
    body.block()
    this.childrenNode += body
    return body
}

fun BlockNode.thead(block: BlockNode.() -> Unit): BlockNode {
    val body = BlockNode("thead")
    body.block()
    this.childrenNode += body
    return body
}

fun BlockNode.tr(block: BlockNode.() -> Unit): BlockNode {
    val body = BlockNode("tr")
    body.block()
    this.childrenNode += body
    return body
}

fun BlockNode.th(block: BlockNode.() -> Unit): BlockNode {
    val body = BlockNode("th")
    body.block()
    this.childrenNode += body
    return body
}

fun BlockNode.p(block: BlockNode.() -> Unit): BlockNode {
    val body = BlockNode("p")
    body.block()
    this.childrenNode += body
    return body
}

fun BlockNode.a(block: BlockNode.() -> Unit): BlockNode {
    val body = BlockNode("a")
    body.block()
    this.childrenNode += body
    return body
}

fun BlockNode.img(block: BlockNode.() -> Unit): BlockNode {
    val body = BlockNode("img")
    body.block()
    this.childrenNode += body
    return body
}

fun BlockNode.td(block: BlockNode.() -> Unit): BlockNode {
    val body = BlockNode("td")
    body.block()
    this.childrenNode += body
    return body
}

fun BlockNode.div(block: BlockNode.() -> Unit): BlockNode {
    val body = BlockNode("div")
    body.block()
    this.childrenNode += body
    return body
}

fun BlockNode.button(block: BlockNode.() -> Unit): BlockNode {
    val body = BlockNode("button")
    body.block()
    this.childrenNode += body
    return body
}

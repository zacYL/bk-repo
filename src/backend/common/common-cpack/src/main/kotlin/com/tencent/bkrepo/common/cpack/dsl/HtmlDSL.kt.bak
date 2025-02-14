package com.tencent.bkrepo.common.cpack.dsl

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

    val childrenNode = mutableListOf<Node>()
    private val properties = mutableMapOf<String, Any>()

    override fun render(step: Int): String {
        val stringBuilder = StringBuilder("")
        for (i in 1 until step) {
            stringBuilder.append("    ")
        }
        val space = stringBuilder.toString()
        return if (properties.isEmpty()) {
            if (childrenNode.isEmpty()) {
                """$space<$name></$name>""".trimMargin()
            } else {
                """$space<$name>
                |${childrenNode.joinToString("\n") { it.render(step.inc()) }}
                |$space</$name>""".trimMargin()
            }
        } else {
            if (childrenNode.isEmpty()) {
                """$space<$name ${properties.map { "${it.key}='${it.value}'" }.joinToString(" ")}>
                |$space</$name>""".trimMargin()
            } else {
                """$space<$name ${properties.map { "${it.key}='${it.value}'" }.joinToString(" ")}>
                |${childrenNode.joinToString("\n") { it.render(step.inc()) }}
                |$space</$name>""".trimMargin()
            }
        }
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

    operator fun String.unaryPlus() {
        this@BlockNode.childrenNode += StringNode(this)
    }

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

fun BlockNode.head(block: BlockNode.() -> Unit): BlockNode {
    val head = BlockNode("head")
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

package com.tencent.bkrepo.common.cpack.mail

import com.tencent.bkrepo.common.cpack.dsl.a
import com.tencent.bkrepo.common.cpack.dsl.body
import com.tencent.bkrepo.common.cpack.dsl.button
import com.tencent.bkrepo.common.cpack.dsl.div
import com.tencent.bkrepo.common.cpack.dsl.head
import com.tencent.bkrepo.common.cpack.dsl.html
import com.tencent.bkrepo.common.cpack.dsl.img
import com.tencent.bkrepo.common.cpack.dsl.p
import com.tencent.bkrepo.common.cpack.dsl.table
import com.tencent.bkrepo.common.cpack.dsl.tbody
import com.tencent.bkrepo.common.cpack.dsl.td
import com.tencent.bkrepo.common.cpack.dsl.th
import com.tencent.bkrepo.common.cpack.dsl.thead
import com.tencent.bkrepo.common.cpack.dsl.tr
import com.tencent.bkrepo.common.cpack.pojo.FileShareInfo
import java.io.File
import java.time.LocalDate

object MailTemplate {

    private const val indent = 13

    fun mainBodyHtml(nest: String): String {
        return table {
            "cellpadding"(0)
            "cellspacing"(0)
            "width"("100%")
            "style"(
                "font-size: 14px; " +
                    "mso-table-lspace: 0pt; " +
                    "mso-table-rspace: 0pt; "
            )
            tr {
                td {
                    "class"("email-source")
                    "style"("padding: 14px 0; color: #bebebe;")
                    +"来自CPACK制品库的通知"
                }
            }
            // 表格内容
            -nest
        }.render(indent)
    }

    fun newAccountHtml(cnName: String, userId: String, url: String): String {
        return tr {
            "class"("email-information")
            td {
                "class"("table-info; line")
                div {
                    "style"("line-height: 40px")
                    +"Hi, $cnName"
                }
                div {
                    "style"("line-height: 40px")
                    +"你的制品库账号: $userId 已经创建, 初始密码请联系管理员获得."
                }
                div {
                    "style"("line-height: 40px")
                    +"请点击下方按钮可登录制品库, 并在个人中心管理您的账户信息与密码"
                }
                button {
                    "style"(
                        "line-height:50px; " +
                            "background-color: #3c96ff;" +
                            "font-size: 32px;" +
                            "border-radius: 8px;border: none"
                    )
                    a {
                        "style"("color: hsl(240, 9%, 98%); text-decoration: none")
                        "href"(url)
                        +"登录制品库"
                    }
                }
                div {
                    "style"("line-height: 80px")
                    +"此致，"
                }
                div {
                    "style"("line-height: 0px")
                    +"CPACK制品库团队"
                }
                div {
                    "style"("line-height: 40px;float: right")
                    +LocalDate.now().toString()
                }
            }
        }.render(indent.inc())
    }

    fun resetPwdHtml(cnName: String, userId: String, url: String): String {
        return tr {
            "class"("email-information")
            td {
                "class"("table-info; line")
                div {
                    "style"("line-height: 40px")
                    +"Hi, $cnName"
                }
                div {
                    "style"("line-height: 40px")
                    +"你的制品库账号: $userId 密码已经重置, 初始密码请联系管理员获得."
                }
                div {
                    "style"("line-height: 40px")
                    +"请点击下方按钮可登录制品库, 并在个人中心管理您的账户信息与密码"
                }
                button {
                    "style"(
                        "line-height:50px; " +
                            "background-color: #3c96ff;" +
                            "font-size: 32px;" +
                            "border-radius: 8px;border: none"
                    )
                    a {
                        "style"("color: hsl(240, 9%, 98%); text-decoration: none")
                        "href"(url)
                        +"登录制品库"
                    }
                }
                div {
                    "style"("line-height: 80px")
                    +"此致，"
                }
                div {
                    "style"("line-height: 0px")
                    +"CPACK制品库团队"
                }
                div {
                    "style"("line-height: 40px;float: right")
                    +LocalDate.now().toString()
                }
            }
        }.render(indent.inc())
    }

    fun fileTableHtml(
        cnName: String,
        projectId: String,
        expireDays: String?,
        shareFileList: List<FileShareInfo>
    ): String {
        return tr {
            "class"("email-information")
            td {
                "class"("table-info")
                table {
                    "cellpadding"(0)
                    "cellspacing"(0)
                    "width"("100%")
                    "style"(
                        "font-size: 14px; " +
                            "mso-table-lspace: 0pt; " +
                            "mso-table-rspace: 0pt;"
                    )
                    tr {
                        "class"("table-title")
                        td {
                            "style"(
                                "padding-top: 36px; " +
                                    "padding-bottom: 14px; " +
                                    "color: #707070;" +
                                    "padding-top: 36px; " +
                                    "padding-bottom: 14px; " +
                                    "color: #707070;"
                            )
                            +"${cnName}与你共享以下文件, 请在有效期 ($expireDays) 内及时下载: "
                        }
                    }
                    tr {
                        td {
                            table {
                                "cellpadding"(0)
                                "cellspacing"(0)
                                "width"("100%")
                                "style"(
                                    "font-size: 14px; " +
                                        "mso-table-lspace: 0pt; " +
                                        "mso-table-rspace: 0pt; " +
                                        "border: 1px solid #e6e6e6; " +
                                        "border-collapse: collapse;"
                                )
                                thead {
                                    "style"("background: #f6f8f8;")
                                    tr {
                                        "style"("color: #333C48;")
                                        th {
                                            "width"("50%")
                                            "style"(
                                                "padding: 16px; " +
                                                    "border: 1px solid #e6e6e6;" +
                                                    "text-align: left; " +
                                                    "font-weight: normal;"
                                            )
                                            +"文件名"
                                        }
                                        th {
                                            "width"("35%")
                                            "style"(
                                                "padding: 16px; " +
                                                    "border: 1px solid #e6e6e6;" +
                                                    "text-align: left; " +
                                                    "font-weight: normal;"
                                            )
                                            +"所属项目"
                                        }
                                        th {
                                            "width"("15%")
                                            "style"(
                                                "padding: 16px; " +
                                                    "border: 1px solid #e6e6e6;" +
                                                    "text-align: center; " +
                                                    "font-weight: normal;"
                                            )
                                            +"操作"
                                        }
                                    }
                                }
                                tbody {
                                    "style"("color: #707070;")
                                    shareFileList.forEach {
                                        tr {
                                            td {
                                                "style"(
                                                    "padding: 16px; " +
                                                        "border: 1px solid #e6e6e6;" +
                                                        "text-align: left; " +
                                                        "font-weight: normal;"
                                                )
                                                p {
                                                    "style"("margin: 0px")
                                                    +it.fileName
                                                }
                                                p {
                                                    "style"("margin: 0px;color: #c7c7c7")
                                                    +"MD5: ${it.md5}"
                                                }
                                            }
                                            td {
                                                "style"(
                                                    "padding: 16px; " +
                                                        "border: 1px solid #e6e6e6;" +
                                                        "text-align: left; " +
                                                        "font-weight: normal"
                                                )
                                                +projectId
                                            }
                                            td {
                                                "style"(
                                                    "padding: 16px; " +
                                                        "border: 1px solid #e6e6e6;" +
                                                        "text-align: center; " +
                                                        "font-weight: normal"
                                                )
                                                a {
                                                    "style"("color: #3c96ff")
                                                    "href"(it.downloadUrl)
                                                    +"下载"
                                                    it.qrCodeBase64?.let {
                                                        img { "src"(it) }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }.render(indent.inc())
    }

    // 邮件基础模板
    fun mailCommonHtml(cnName: String, projectId: String, nest: String): String {
        return html {
            head {
                "meta" { "charset"("UTF-8") }
            }
            body {
                table {
                    "class"("template-table")
                    "cellpadding"(0)
                    "cellspacing"(0)
                    "width"("100%")
                    "style"(
                        "font-size: 14px; " +
                            "min-width: auto; " +
                            "mso-table-lspace: 0pt; " +
                            "mso-table-rspace: 0pt; " +
                            "background-color: #fff; " +
                            "border: #3c96ff"
                    )
                    tbody {
                        tr {
                            td {
                                "align"("center")
                                "valign"("top")
                                "width"("100%")
                                "style"("padding: 16px")
                                table {
                                    "class"("template-table")
                                    "cellpadding"(0)
                                    "cellspacing"(0)
                                    "width"("956")
                                    "style"(
                                        "font-size: 14px; " +
                                            "min-width: auto; " +
                                            "mso-table-lspace: 0pt; " +
                                            "mso-table-rspace: 0pt; " +
                                            "background-color: #fff; " +
                                            "border: #3c96ff"
                                    )
                                    tbody {
                                        tr {
                                            "style"("height: 64px; background: rgb(35, 120, 199);")
                                            td {
                                                "style"("padding-left: 24px;")
                                                "width"("60")
                                                div {
                                                    "style"("font-size:35px;color: hsl(240, 9%, 98%);")
                                                    +"CPACK制品库"
                                                }
                                            }
                                        }
                                        tr {
                                            td {
                                                "align"("center")
                                                "valign"("top")
                                                "style"("padding: 24px;background-color: #ccdbe0;")
                                                table {
                                                    "cellpadding"(0)
                                                    "cellspacing"(0)
                                                    "width"("100%")
                                                    "style"(
                                                        "font-size: 14px; " +
                                                            "mso-table-lspace: 0pt; " +
                                                            "mso-table-rspace: 0pt; " +
                                                            "border: 1px solid #e6e6e6;"
                                                    )
                                                    tr {
                                                        td {
                                                            "class"("email-title")
                                                            "style"(
                                                                "padding: 20px 36px; " +
                                                                    "line-height: 1.5; " +
                                                                    "border-bottom: 1px solid #e6e6e6; " +
                                                                    "background: #fff; " +
                                                                    "font-size: 22px;"
                                                            )
                                                            +"【CPACK】${cnName}与你共享以下文件"
                                                        }
                                                    }
                                                    tr {
                                                        td {
                                                            "class"("email-content")
                                                            "style"("padding: 0 36px; background: #fff;")
                                                            // 此处 render(13)
                                                            -nest
                                                        }
                                                    }
                                                    tr {
                                                        "class"("email-footer")
                                                        td {
                                                            "style"(
                                                                "padding: 20px 0 20px 36px; " +
                                                                    "border-top: 1px solid #e6e6e6; " +
                                                                    "background: #fff; " +
                                                                    "color: #c7c7c7;"
                                                            )
                                                            +"你收到此邮件, 是因为你关注了 $projectId 项目, 或其它人@了你"
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }.render()
    }

    fun mailCommonHtml2(nest: String): String {
        return html {
            head {
                "meta" { "charset"("UTF-8") }
            }
            body {
                table {
                    "class"("template-table")
                    "cellpadding"(0)
                    "cellspacing"(0)
                    "width"("100%")
                    "style"(
                        "font-size: 14px; " +
                            "min-width: auto; " +
                            "mso-table-lspace: 0pt; " +
                            "mso-table-rspace: 0pt; " +
                            "background-color: #fff; " +
                            "border: #3c96ff"
                    )
                    tbody {
                        tr {
                            td {
                                "align"("center")
                                "valign"("top")
                                "width"("100%")
                                "style"("padding: 16px")
                                table {
                                    "class"("template-table")
                                    "cellpadding"(0)
                                    "cellspacing"(0)
                                    "width"("956")
                                    "style"(
                                        "font-size: 14px; " +
                                            "min-width: auto; " +
                                            "mso-table-lspace: 0pt; " +
                                            "mso-table-rspace: 0pt; " +
                                            "background-color: #fff; " +
                                            "border: #3c96ff"
                                    )
                                    tbody {
                                        tr {
                                            "style"("height: 64px; background: rgb(35, 120, 199);")
                                            td {
                                                "style"("padding-left: 24px;")
                                                "width"("60")
                                                div {
                                                    "style"("font-size:35px;color: hsl(240, 9%, 98%);")
                                                    +"CPACK制品库"
                                                }
                                            }
                                        }
                                        tr {
                                            td {
                                                "align"("center")
                                                "valign"("top")
                                                "style"("padding: 24px;background-color: #ccdbe0;")
                                                table {
                                                    "cellpadding"(0)
                                                    "cellspacing"(0)
                                                    "width"("100%")
                                                    "style"(
                                                        "font-size: 14px; " +
                                                            "mso-table-lspace: 0pt; " +
                                                            "mso-table-rspace: 0pt; " +
                                                            "border: 1px solid #e6e6e6;"
                                                    )
                                                    // tr {
                                                    //    td {
                                                    //        "class"("email-title")
                                                    //        "style"(
                                                    //            "padding: 20px 36px; " +
                                                    //                    "line-height: 1.5; " +
                                                    //                    "border-bottom: 1px solid #e6e6e6; " +
                                                    //                    "background: #fff; " +
                                                    //                    "font-size: 22px;"
                                                    //        )
                                                    //        +"【CPACK】${cnName}与你共享以下文件"
                                                    //    }
                                                    // }
                                                    tr {
                                                        td {
                                                            "class"("email-content")
                                                            "style"("padding: 0 36px; background: #fff;")
                                                            // 此处 render(13)
                                                            -nest
                                                        }
                                                    }
                                                    // tr {
                                                    //    "class"("email-footer")
                                                    //    td {
                                                    //        "style"(
                                                    //            "padding: 20px 0 20px 36px; " +
                                                    //                    "border-top: 1px solid #e6e6e6; " +
                                                    //                    "background: #fff; " +
                                                    //                    "color: #c7c7c7;"
                                                    //        )
                                                    //        +"你收到此邮件, 是因为你关注了 $projectId 项目, 或其它人@了你"
                                                    //    }
                                                    // }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }.render()
    }
}

fun main() {
    val str = MailTemplate.mailCommonHtml2(
        nest = MailTemplate.mainBodyHtml(
            MailTemplate.newAccountHtml(
                cnName = "李章铭",
                userId = "weaving",
                url = "https://127.0.0.1"
            )
        )
    )
    File("/Users/weaving/Desktop/account.html").writeText(str)
}

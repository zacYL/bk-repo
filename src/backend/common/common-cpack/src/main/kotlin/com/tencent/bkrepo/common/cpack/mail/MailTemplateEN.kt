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

object MailTemplateEN {

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
                    +"Notifications from the CPack Artifact Library"
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
                    +buildString {
        append("Your Artifact Library account: ")
        append(userId)
        append(" has been created, please contact the administrator to obtain the initial password.")
    }
                }
                div {
                    "style"("line-height: 40px")
                    +"Please click the button below to log in to the Product library and manage your account information and password in the Personal Center"
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
                        +"Login to the Artifact Library"
                    }
                }
                div {
                    "style"("line-height: 80px")
                    +"Sincerely,"
                }
                div {
                    "style"("line-height: 0px")
                    +" CPack product library team"
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
                            +buildString {
        append(cnName)
        append(" shares the following files with you, please download them in time within the validity period (")
        append(expireDays)
        append("): ")
    }
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
                                            +"File Name"
                                        }
                                        th {
                                            "width"("35%")
                                            "style"(
                                                "padding: 16px; " +
                                                        "border: 1px solid #e6e6e6;" +
                                                        "text-align: left; " +
                                                        "font-weight: normal;"
                                            )
                                            +"Project Operation"
                                        }
                                        th {
                                            "width"("15%")
                                            "style"(
                                                "padding: 16px; " +
                                                        "border: 1px solid #e6e6e6;" +
                                                        "text-align: center; " +
                                                        "font-weight: normal;"
                                            )
                                            +"Download"
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
                                                    +"Download"
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
                                                    +"CPack Artifact Library"
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
                                                            +"【CPack】$cnName shares the following files with you"
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
                                                            +"You receive this email because you followed the project of $projectId, or other people @ you"
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
                                                    +"CPack Artifact Library"
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
                                                    //        +"【CPack】${cnName}与你共享以下文件"
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
    val str = MailTemplateEN.mailCommonHtml2(
        nest = MailTemplateEN.mainBodyHtml(
            MailTemplateEN.newAccountHtml(
                cnName = "李章铭",
                userId = "weaving",
                url = "https://127.0.0.1"
            )
        )
    )
    File("/Users/weaving/Desktop/account.html").writeText(str)
}

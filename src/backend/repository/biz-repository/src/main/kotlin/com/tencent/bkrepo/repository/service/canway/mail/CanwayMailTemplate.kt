package com.tencent.bkrepo.repository.service.canway.mail

import com.tencent.bkrepo.repository.service.canway.pojo.FileShareInfo
import java.text.SimpleDateFormat
import java.util.Date

object CanwayMailTemplate {

    fun getShareEmailTitle(cnName: String, fileName: String): String {
        return "【BKREPO仓库通知】${cnName}与你共享${fileName}文件"
    }

    fun getShareEmailBody(projectId: String, title: String, cnName: String, days: Int, FileShareInfoList: List<FileShareInfo>): String {
        val simpleDateFormat = SimpleDateFormat("yyyy年MM月dd日")
        val date = simpleDateFormat.format(Date())
        val stringBuffer = StringBuilder()
        stringBuffer.append(SHARE_EMAIL_HTML_PREFIX)
        FileShareInfoList.forEach {
            stringBuffer.append(getShareEmailBodyRow(it.fileName, it.md5, it.projectId, it.downloadUrl, it.qrCodeBase64))
        }
        stringBuffer.append(SHARE_EMAIL_HTML_SUFFIX)
        val template = stringBuffer.toString()
        return template
                .replace(HEADER_TITLE_TEMPLATE, title)
                .replace(BODY_TITLE_TEMPLATE, "${cnName}与你共享以下文件，请在有效期（${days}天）内及时下载：")
                .replace(BODY_PROJECT_TEMPLATE, projectId)
                .replace(BODY_DATE_TEMPLATE, date)
                .replace(TABLE_COLUMN1_TITLE, "文件名")
                .replace(TABLE_COLUMN2_TITLE, "所属项目")
                .replace(TABLE_COLUMN3_TITLE, "操作")
    }

    private fun getShareEmailBodyRow(fileName: String, md5: String, projectName: String, downloadUrl: String, qrCodeBase64: String): String {
        return "                                                                            <tr>\n" +
                "                                                                                <td style=\"padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\">" +
                "                                                                                   <p style=\"margin: 0px\">$fileName</p>\n" +
                "                                                                                   <p style=\"margin: 0px;color: #c7c7c7\">MD5：$md5</p>\n" +
                "                                                                                </td>\n" +
                "                                                                                <td style=\"padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\">$projectName</td>\n" +
                "                                                                                <td style=\"padding: 16px; border: 1px solid #e6e6e6;text-align: center; font-weight: normal;\"><a href=\"$downloadUrl\" style=\"color: #3c96ff\">下载</a><img src=\"$qrCodeBase64\"></td>\n" +
                "                                                                            </tr>\n"
    }

    private val HEADER_TITLE_TEMPLATE = "#{headerTitle}"
    private val BODY_PROJECT_TEMPLATE = "#{bodyProject}"
    private val BODY_TITLE_TEMPLATE = "#{bodyTitle}"
    private val BODY_DATE_TEMPLATE = "#{bodyDate}"
    private val TABLE_COLUMN1_TITLE = "#{column1Title}"
    private val TABLE_COLUMN2_TITLE = "#{column2Title}"
    private val TABLE_COLUMN3_TITLE = "#{column3Title}"

    private val SHARE_EMAIL_HTML_PREFIX = "<table class=\"template-table\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; min-width: auto; mso-table-lspace: 0pt; mso-table-rspace: 0pt; background-color: #fff; background: #fff;\">\n" +
            "    <tbody>\n" +
            "        <tr>\n" +
            "            <td align=\"center\" valign=\"top\" width=\"100%\" style=\"padding: 16px;\">\n" +
            "               <table class=\"template-table\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"956\" style=\"font-size: 14px; min-width: auto; mso-table-lspace: 0pt; mso-table-rspace: 0pt;\">\n" +
            "                    <tbody>\n" +
            "                        <tr>\n" +
            "                            <td valign=\"top\" align=\"center\">\n" +
            "                                <table cellpadding=\"0\" cellspacing=\"0\" border=\"0\" bgcolor=\"#f9f8f6\" class=\"layout layout-table root-table\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt;\">\n" +
            "                                    <tbody>\n" +
            "                                        <tr style=\"height: 64px; background: #555;\">\n" +
            "                                            <td style=\"padding-left: 24px;\" width=\"60\" align=\"center\">\n" +
//            "                                                <img src=\"email/logo.png\" width=\"52\" style=\"display: block\">\n" +
            "                                            </td>\n" +
            "                                            <td style=\"padding-left: 6px;\">\n" +
//            "                                                <img src=\"email/title.png\" width=\"176\" style=\"display: block\">\n" +
            "                                            </td>\n" +
            "                                        </tr>\n" +
            "                                    </tbody>\n" +
            "                                </table>\n" +
            "                            </td>\n" +
            "                        </tr>\n" +
            "                        <tr>\n" +
            "                            <td valign=\"top\" align=\"center\" style=\"padding: 24px;\" bgcolor=\"#f9f8f6\">\n" +
            "                                <table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt; border: 1px solid #e6e6e6;\">\n" +
            "                                    <tr>\n" +
            "                                        <td class=\"email-title\" style=\"padding: 20px 36px; line-height: 1.5; border-bottom: 1px solid #e6e6e6; background: #fff; font-size: 22px;\">$HEADER_TITLE_TEMPLATE</td>\n" +
            "                                    </tr>\n" +
            "                                    <tr>\n" +
            "                                        <td class=\"email-content\" style=\"padding: 0 36px; background: #fff;\">\n" +
            "                                            <table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt;\">\n" +
            "                                                <tr>\n" +
            "                                                    <td class=\"email-source\" style=\"padding: 14px 0; color: #bebebe;\">来自蓝鲸DevOps平台的推送</td>\n" +
            "                                                </tr>\n" +
            "                                                <!-- 表格内容 -->\n" +
            "                                                <tr class=\"email-information\">\n" +
            "                                                    <td class=\"table-info\">\n" +
            "                                                        <table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt;\">\n" +
            "                                                            <tr class=\"table-title\">\n" +
            "                                                                <td style=\"padding-top: 36px; padding-bottom: 14px; color: #707070;\">$BODY_TITLE_TEMPLATE</td>\n" +
            "                                                            </tr>\n" +
            "                                                            <tr>\n" +
            "                                                                <td>\n" +
            "                                                                    <table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"font-size: 14px; mso-table-lspace: 0pt; mso-table-rspace: 0pt; border: 1px solid #e6e6e6; border-collapse: collapse;\">\n" +
            "                                                                        <thead style=\"background: #f6f8f8;\">\n" +
            "                                                                            <tr style=\"color: #333C48;\">\n" +
            "                                                                                <th width=\"50%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\">$TABLE_COLUMN1_TITLE</th>\n" +
            "                                                                                <th width=\"35%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: left; font-weight: normal;\">$TABLE_COLUMN2_TITLE</th>\n" +
            "                                                                                <th width=\"15%\" style=\" padding: 16px; border: 1px solid #e6e6e6;text-align: center; font-weight: normal;\">$TABLE_COLUMN3_TITLE</th>\n" +
            "                                                                            </tr>\n" +
            "                                                                        </thead>\n" +
            "                                                                        <tbody style=\"color: #707070;\">\n"

    private val SHARE_EMAIL_HTML_SUFFIX =
            "                                                                        </tbody>\n" +
                    "                                                                    </table>\n" +
                    "                                                                </td>\n" +
                    "                                                            </tr>\n" +
                    "                                                        </table>\n" +
                    "                                                    </td>\n" +
                    "                                                </tr>\n" +
                    "\n" +
                    "                                                <tr class=\"prompt-tips\">\n" +
                    "                                                    <td style=\"padding-top: 32px; padding-bottom: 10px; color: #707070;\">如有任何问题，可随时联系蓝盾助手。</td>\n" +
                    "                                                </tr>\n" +
                    "                                                <tr class=\"info-remark\">\n" +
                    "                                                    <td style=\"padding: 20px 0; text-align: right; line-height: 24px; color: #707070;\">\n" +
                    "                                                        <div>$BODY_DATE_TEMPLATE</div>\n" +
                    "                                                    </td>\n" +
                    "                                                </tr>\n" +
                    "                                            </table>\n" +
                    "                                        </td>\n" +
                    "                                    </tr>\n" +
                    "                                    <tr class=\"email-footer\">\n" +
                    "                                        <td style=\" padding: 20px 0 20px 36px; border-top: 1px solid #e6e6e6; background: #fff; color: #c7c7c7;\">你收到此邮件，是因为你关注了 $BODY_PROJECT_TEMPLATE 项目，或其它人@了你</td>\n" +
                    "                                    </tr>\n" +
                    "                                </table>\n" +
                    "                            </td>\n" +
                    "                        </tr>\n" +
                    "                    </tbody>\n" +
                    "               </table>\n" +
                    "            </td>\n" +
                    "        </tr>\n" +
                    "    </tbody>\n" +
                    "</table>\n"
}

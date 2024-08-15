export default () => `

* API: PUT /api/remote/whitelist/batch/

* API 名称: insert_remote_package_whitelist_batch

* 功能说明：

  - 中文：批量新增代理制品白名单
  - English：batch create remote package whitelist

* 请求体

  \`\`\`json
  [
      {
          "packageKey":"com.alibaba:fastjson",
          "versions":["1.0","2.0"],
          "type":"MAVEN"
      },
      {
          "packageKey":"net.canway:bkrepo",
          "versions":["1.1","2.2"],
          "type":"MAVEN"
      }
  ]
  \`\`\`

* 请求字段说明

  | 字段       | 类型         | 默认值 | 是否必传 | 说明                                               | Description     |
  | ---------- | ------------ | ------ | -------- | -------------------------------------------------- | --------------- |
  | packageKey | String       | 无     | 是       | 唯一包名(不同仓库类型有不同校验规则，参考文档声明) | package key     |
  | versions   | List<String> | 无     | 否       | 新增的白名单版本                                   | versions        |
  | type       | String       | 无     | 是       | 仓库类型                                           | repository type |

* 响应体

  \`\`\`json
  {
      "code": 0,
      "message": null,
      "data": 2,
      "traceId": ""
  }
  \`\`\`

* data字段说明

  | 字段 | 类型 | 说明               | Description                |
  | ---- | ---- | ------------------ | -------------------------- |
  | data | Int  | 批量操作成功数统计 | batch insert success count |
`

export default () => `

* API: PUT /api/remote/whitelist/batch/

* API Name: insert_remote_package_whitelist_batch

* Function Description:

  - batch create remote package whitelist

* Request body

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

* Request field description

  | Field       | Type         | Default value | Is it necessary | explanation                                        | Description     |
  | ----------- | ------------ | ------------- | --------------- | -------------------------------------------------- | --------------- |
  | packageKey  | String       | none          | yes             | Unique package name (different repository types have different verification rules, refer to the document statement)     | package key     |
  | versions    | List<String> | none          | no              | New whitelist version                              | versions        |
  | type        | String       | none          | yes             | Repository type                                    | repository type |

* Response body

  \`\`\`json
  {
      "code": 0,
      "message": null,
      "data": 2,
      "traceId": ""
  }
  \`\`\`

* data field description

  | Field | Type | explanation                          | Description                |
  | ----- | ---- | ------------------------------------ | -------------------------- |
  | data  | Int  | Batch operation success statistics   | batch insert success count |
`

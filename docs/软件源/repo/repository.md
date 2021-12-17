# Repository仓库接口

[toc]

## 软件源分页查询仓库

- API: GET /repository/api/software/page/{pageNumber}/{pageSize}?projectId=test&name=local&type=GENERIC
- API 名称: list_software_repo_page
- 功能说明：
  - 中文：分页查询软件源仓库
  - English：list software repo page
- 请求体
  此接口无请求体
- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |projectId|string|否|无|项目名称|project name|
  |pageNumber|int|是|无|当前页|page number|
  |pageSize|int|是|无|分页数量|page size|
  |name|string|否|无|仓库名称，支持前缀模糊匹配|repo name|
  |type|string|否|无|仓库类型，枚举值|repo type|

- 响应体

  ```json
  {
      "code": 0,
      "message": null,
      "data": {
          "pageNumber": 1,
          "pageSize": 20,
          "totalRecords": 1,
          "totalPages": 1,
          "records": [
              {
                  "projectId": "test",
                  "name": "maven1",
                  "type": "MAVEN",
                  "category": "LOCAL",
                  "public": false,
                  "description": null,
                  "configuration": {
                      "type": "local",
                      "webHook": {
                          "webHookList": []
                      },
                      "settings": {
                          "system": true
                      }
                  },
                  "storageCredentialsKey": null,
                  "createdBy": "admin",
                  "createdDate": "2021-12-14T13:44:36.582",
                  "lastModifiedBy": "admin",
                  "lastModifiedDate": "2021-12-14T13:44:36.582",
                  "hasPermission": null,
                  "permission": null,
                  "artifacts": null,
                  "quota": null,
                  "used": 0
              }
          ],
          "page": 1,
          "count": 1
      },
      "traceId": ""
  }
  ```

- data字段说明

  |字段|类型|说明|Description|
  |---|---|---|---|
  |projectId|string|项目id|project id|
  |name|string|仓库名称|repo name|
  |type|string|仓库类型|repo type|
  |category|string|仓库类别|repo category|
  |public|boolean|是否公开仓库（代表完全公开）|is public repo|
  |settings.system|boolean|是否项目级仓库|System|
  |description|string|仓库描述|repo description|
  |createdBy|string|创建者|create user|
  |createdDate|string|创建时间|create time|
  |lastModifiedBy|string|上次修改者|last modify user|
  |lastModifiedDate|string|上次修改时间|last modify time|
  |quota|long|仓库配额，单位字节，值为nul时表示未设置仓库配额|repo quota|
  |used|long|仓库已使用容量，单位字节|repo used volume|


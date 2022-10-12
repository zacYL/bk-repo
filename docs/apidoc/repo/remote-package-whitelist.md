# CVE Whitelist 

[toc]

## 服务模块名

module: **repository**



## packageKey规则：

校验规则如下：

```txt
MAVEN: [a-zA-Z0-9_\-.]+:[a-zA-Z0-9_\-.]+
NPM：todo
```



## 可选仓库类型

* API: GET /api/remote/whitelist/optional/type

* API 名称: get_optional_type

* 功能说明：

  * 中文： 获取可选仓库类型
  * English: get optional type

* 请求体

  此接口请求体为空

* 请求字段说明

  无

* 响应体

  ```json
  {
      "code": 0,
      "message": null,
      "data": [
          "MAVEN"
      ],
      "traceId": ""
  }
  ```

  



## 	新增代理制品白名单

* API: PUT /api/remote/whitelist

* API 名称: insert_remote_package_whitelist

- 功能说明：
  - 中文：新增代理制品白名单
  - English：create remote package whitelist
  
* 请求体

  ```json
  {
      "packageKey":"com.alibaba:fastjson",
      "versions":["1.0","2.0"],
      "type":"MAVEN"
  }
  ```

  

* 请求字段说明

  | 字段       | 类型         | 默认值 | 是否必传 | 说明                                               | Description     |
  | ---------- | ------------ | ------ | -------- | -------------------------------------------------- | --------------- |
  | packageKey | String       | 无     | 是       | 唯一包名(不同仓库类型有不同校验规则，参考文档声明) | package key     |
  | versions   | List<String> | 无     | 否       | 新增的白名单版本                                   | versions        |
  | type       | String       | 无     | 是       | 仓库类型                                           | repository type |

* 响应体

  ```json
  {
      "code": 0,
      "message": null,
      "data": true,
      "traceId": ""
  }
  ```

  

## 	删除代理制品白名单

* API: DELETE /api/remote/whitelist/{id}

* API 名称: delete_remote_package_whitelist

* 功能说明：

  - 中文：删除代理制品白名单
  - English：delete remote package whitelist

* 请求体

  此接口请求体为空

* 请求字段说明

  | 字段 | 类型   | 默认值 | 是否必传 | 说明 | Description |
  | ---- | ------ | ------ | -------- | ---- | ----------- |
  | id   | String | 无     | 是       | id   | id          |

* 响应体

  ```json
  {
      "code": 0,
      "message": null,
      "data": null,
      "traceId": ""
  }
  ```






## 	批量新增代理制品白名单

* API: PUT /api/remote/whitelist/batch/

* API 名称: insert_remote_package_whitelist_batch

* 功能说明：

  - 中文：批量新增代理制品白名单
  - English：batch create remote package whitelist

* 请求体

  ```json
  [
      {
          "packageKey":"conm.alibaba:fastjson",
          "versions":["1.0","2.0"],
          "type":"MAVEN"
  	},
      {
          "packageKey":"net.canway:bkrepo",
          "versions":["1.1","2.2],
          "type":"MAVEN"
  	}
  ]
  ```

* 请求字段说明

  | 字段       | 类型         | 默认值 | 是否必传 | 说明                                               | Description     |
  | ---------- | ------------ | ------ | -------- | -------------------------------------------------- | --------------- |
  | packageKey | String       | 无     | 是       | 唯一包名(不同仓库类型有不同校验规则，参考文档声明) | package key     |
  | versions   | List<String> | 无     | 否       | 新增的白名单版本                                   | versions        |
  | type       | String       | 无     | 是       | 仓库类型                                           | repository type |

* 响应体

  ```json
  {
      "code": 0,
      "message": null,
      "data": 2,
      "traceId": ""
  }
  ```


* data字段说明

  | 字段 | 类型 | 说明               | Description                |
  | ---- | ---- | ------------------ | -------------------------- |
  | data | Int  | 批量操作成功数统计 | batch insert success count |



## 	获取代理制品白名单详情

* API: GET /api/remote/whitelist/{id}

* API 名称: get_remote_package_whitelist

* 功能说明：

  - 中文：获取代理制品白名单详情
  - English：get remote package whitelist

* 请求体

  此接口请求体为空

* 请求字段说明

  | 字段 | 类型   | 默认值 | 是否必传 | 说明 | Description |
  | ---- | ------ | ------ | -------- | ---- | ----------- |
  | id   | String | 无     | 是       | id   | id          |

* 响应体

  ```json
  {
      "code": 0,
      "message": null,
      "data": {
          "id": "632c1b4d7d5f1817d08daf6c",
          "packageKey": "com.alibaba:fastjson",
          "versions": ["1.0","2.0"],
          "type": "MAVEN",
          "createdBy": "anonymous",
          "createdDate": "2022-09-22T16:22:37.859",
          "lastModifiedBy": "anonymous",
          "lastModifiedDate": "2022-09-23T10:52:03.053"
      },
      "traceId": ""
  }
  ```
  
* data字段说明

  | 字段             | 类型         | 说明         | Description      |
  | ---------------- | ------------ | ------------ | ---------------- |
  | id               | string       | id           | id               |
  | packageKey       | string       | 唯一包名     | package key      |
  | type             | String       | 仓库类型     | repository type  |
  | versions         | List<String> | 版本集合     | versions         |
  | createdBy        | string       | 创建者       | create user      |
  | createdDate      | string       | 创建时间     | create time      |
  | lastModifiedBy   | string       | 上次修改者   | last modify user |
  | lastModifiedDate | string       | 上次修改时间 | last modify time |





## 	代理制品白名单 分页查询

* API: GET /api/remote/whitelist/page?type={repositoryType}&packageKey={packageKey}&version={version}&pageNumber={pageNumber}&pageSize={pageSize}&regex=true

* API 名称: remote_package_whitelist_page

* 功能说明：

  - 中文：代理制品白名单 分页查询
  - English：remote package whitelist page

* 请求体

  此接口请求体为空

* 请求字段说明

  | 字段       | 类型    | 默认值 | 是否必传 | 说明                                | Description     |
  | ---------- | ------- | ------ | -------- | ----------------------------------- | --------------- |
  | type       | String  | 无     | 否       | 仓库类型                            | Repository type |
  | packageKey | String  | 无     | 否       | 唯一包名                            | package key     |
  | version    | String  | 无     | 否       | 版本                                | version         |
  | regex      | Boolean | true   | 否       | true:packageKey正则匹配，false:相等 | regex           |
  | pageNumber | Int     | 1      | 否       | 页数                                | page number     |
  | pageSize   | Int     | 20     | 否       | 每页数量                            | page size       |

* 响应体

  ```json
  {
      "code": 0,
      "message": null,
      "data": {
          "pageNumber": 1,
          "pageSize": 20,
          "totalRecords": 2,
          "totalPages": 1,
          "records": [
              {
                  "id": "632c1b4d7d5f1817d08daf6cd",
                  "packageKey": "net.canway:bkrepo",
                  "versions": ["1.1","2.1"],
                  "type": "MAVEN",
                  "createdBy": "anonymous",
                  "createdDate": "2022-09-22T16:22:37.859",
                  "lastModifiedBy": "anonymous",
                  "lastModifiedDate": "2022-09-23T10:52:03.053"
              },
              {
                  "id": "632c1b4d7d5f1817d08daf6c",
                  "packageKey": "com.alibaba:fastjson",
                  "versions": ["1.0","2.0"],
                  "type": "MAVEN",
                  "createdBy": "anonymous",
                  "createdDate": "2022-09-22T16:22:37.859",
                  "lastModifiedBy": "anonymous",
                  "lastModifiedDate": "2022-09-23T10:52:03.053"
              }
          ],
          "page": 1,
          "count": 2
      },
      "traceId": ""
  }
  ```

* data字段说明

  | 字段             | 类型         | 说明         | Description      |
  | ---------------- | ------------ | ------------ | ---------------- |
  | id               | string       | id           | id               |
  | packageKey       | string       | 唯一包名     | package key      |
  | type             | String       | 仓库类型     | repository type  |
  | versions         | List<String> | 版本集合     | versions         |
  | createdBy        | string       | 创建者       | create user      |
  | createdDate      | string       | 创建时间     | create time      |
  | lastModifiedBy   | string       | 上次修改者   | last modify user |
  | lastModifiedDate | string       | 上次修改时间 | last modify time |



## 	编辑代理制品白名单

* API: POST /api/remote/whitelist/{id}

* API 名称: update_remote_package_whitelist

- 功能说明：
  - 中文：编辑代理制品白名单
  - English：update remote package whitelist

* 请求体

  ```json
  {
      "packageKey":"com.alibaba:fastjson",
      "versions":["1.0","2.0"],
      "type":"MAVEN"
  }
  ```

  

* 请求字段说明

  | 字段       | 类型         | 默认值 | 是否必传 | 说明                                               | Description     |
  | ---------- | ------------ | ------ | -------- | -------------------------------------------------- | --------------- |
  | id         | String       | 无     | 是       | id                                                 | id              |
  | packageKey | String       | 无     | 否       | 唯一包名(不同仓库类型有不同校验规则，参考文档声明) | package key     |
  | versions   | List<String> | 无     | 否       | 白名单版本                                         | versions        |
  | type       | String       | 无     | 否       | 仓库类型                                           | repository type |

* 响应体

  ```json
  {
      "code": 0,
      "message": null,
      "data": true,
      "traceId": ""
  }
  ```




## 仓库类型是否开启拦截清单

* API: GET /api/remote/whitelist/switch/list

* API 名称: get_switch_list

* 功能说明：

  * 中文： 获取仓库类型是否开启拦截清单
  * English: get switch list

* 请求体

  此接口请求体为空

* 请求字段说明

  无

* 响应体

  ```json
  {
      "code": 0,
      "message": null,
      "data": {
          "MAVEN": true,
          "NPM": false
      },
      "traceId": ""
  }
  ```
  
* data字段说明

  | 字段           | 类型    | 说明                        | Description  |
  | -------------- | ------- | --------------------------- | ------------ |
  | RepositoryType | Boolean | true: 开启拦截，false: 关闭 | 是否开启拦截 |



## 修改仓库类型拦截状态

* API: GET /api/remote/whitelist/switch/{RepositoryType}

* API 名称: switch_status

* 功能说明：

  * 中文： 修改仓库类型拦截状态
  * English: switch status

* 请求体

  此接口请求体为空

* 请求字段说明

  | 字段           | 类型   | 默认值 | 是否必传 | 说明     | Description     |
  | -------------- | ------ | ------ | -------- | -------- | --------------- |
  | RepositoryType | String | 无     | 是       | 仓库类型 | repository type |


* 响应体

  ```json
  {
      "code": 0,
      "message": null,
      "data": true,
      "traceId": ""
  }
  ```

* data字段说明

  | 字段 | 类型    | 说明                 | Description      |
  | ---- | ------- | -------------------- | ---------------- |
  | data | Boolean | 返回修改成功后的结果 | 修改成功后的结果 |

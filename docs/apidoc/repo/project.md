# Project项目接口

[toc]

## 创建项目

- API: POST /repository/api/project/create
- API 名称: create_project
- 功能说明：
  - 中文：创建项目
  - English：create project
- 请求体

  ```json
  {
    "name": "test",
    "displayName": "test",
    "description": "project description"
  }
  ```

- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |name|string|是|无|项目名称，要求以字母或者下划线开头，长度不超过32位|proejct name|
  |displayName|string|是|无|显示名称，要求以字母或者下划线开头，长度不超过32位。此字段保留作用，和name设置为相同值即可|project display name|
  |description|string|是|无|项目描述|project description|

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": null,
    "traceId": null
  }
  ```

## 查询项目列表

- API: GET /repository/api/project/list?pageNumber=1&pageSize=10&sortProperty=name&direction=DESC
- API 名称: get_project_list
- 功能说明：
  - 中文：查询项目列表
  - English：get project list
- 请求体
  此接口请求体为空
- 请求字段说明

  |字段|类型|是否必须|默认值|说明|Description|
  |---|---|---|---|---|---|
  |names|string|否|无|项目名称，多个以,隔开|project name|
  |displayNames|string|否|无|显示名称，多个以,隔开|project display name|
  |pageSize|int|否|无|分页数量|page size|
  |pageNumber|int|否|无|当前页|page number|
  |sortProperty|string|否|无|排序字段|sort property|
  |direction|string|否|无|排序方向(ASC或DESC)|direction|
  - 响应体

    ``` json
    {
        "code":0,
        "message":"",
        "data":[
            {
                "name":"project1",
                "displayName":"project1",
                "description":"project1",
                "createdBy": "user",
                "createdDate": "2019-12-20T10:32:51.89",
                "lastModifiedBy": "user",
                "lastModifiedDate": "2019-12-20T10:32:51.89"
            }
        ],
        "traceId": null
    }
    ```

- data 字段说明

  | 字段|类型| 说明     | Description                       |
    |---|---|--------|-----------------------------------|
  |name|string| 项目名    | the project name                  |
  |displayName|string| 项目展示名称 | the display name of project       |
  |description|string| 项目描述   | the description of project        |
  |createdBy|string| 创建人    | the creator of project            |
  |createdDate|string| 创建时间   | the create date of project        |
  |lastModifiedBy|string| 最后修改人  | the last modified of project            |
  |lastModifiedDate|string| 最后修改时间   | the last modified date of project |


## 编辑项目

- API: PUT /repository/api/project/{projectId}

- API 名称: create_project

- 功能说明：

  - 中文：创建项目
  - English：create project

- 请求体

  ```json
  {
    "displayName":"abcdfg",
    "description":"fsffdff"
  }
  ```

- 请求字段说明

  | 字段        | 类型   | 是否必须 | 默认值 | 说明                                                         | Description          |
  | ----------- | ------ | -------- | ------ | ------------------------------------------------------------ | -------------------- |
  | projectId   | string | 是       | 无     |                                                              | proejct name         |
  | displayName | string | 否       | 无     | 显示名称，要求以字母或者下划线开头，长度不超过32位。此字段保留作用，和name设置为相同值即可 | project display name |
  | description | string | 否       | 无     | 项目描述                                                     | project description  |

- 响应体

  ```json
  {
      "code": 0,
      "message": null,
      "data": true,
      "traceId": ""
  }
  ```

- data 字段说明

  | 字段 | 类型    | 说明      | Description      |
  | ---- | ------- | --------- | ---------------- |
  | data | boolean | true:更新 | the project name |



## 校验项目信息是否存在

- API: GET /repository/api/project/exist?name=abc&displayName=abc

- API 名称: check_project_message_exist

- 功能说明：

  - 中文：校验项目信息是否存在
  - English：check project message exist

- 请求体
  此接口无请求体

- 请求字段说明

  | 字段        | 类型   | 是否必须 | 默认值 | 说明         | Description         |
  | ----------- | ------ | -------- | ------ | ------------ | ------------------- |
  | name        | string | 否       | 无     | 项目名称     | project name        |
  | displayName | string | 否       | 无     | 项目显示名称 | project displayName |

- 响应体

  ```json
  {
    "code": 0,
    "message": null,
    "data": true,
    "traceId": null
  }
  ```

- data字段说明

  | 字段 | 类型    | 说明                               | Description       |
  | ---- | ------- | ---------------------------------- | ----------------- |
  | data | boolean | true:资源已存在, false: 资源不存在 | repo exist or not |



## 查询项目依赖源仓库统计数据

- API：GET /repository/api/project/statistics/summary

- API名称：get_repository_project_statistics

- 功能说明：

  - 中文：查询项目统计数据
  - English: get project statistics

- 请求体 此接口无请求体

- 请求字段说明

  | 字段      | 类型   | 是否必须 | 默认值 | 说明     | Description |
  | --------- | ------ | -------- | ------ | -------- | ----------- |
  | projectId | String | 是       | 无     | 项目ID   | Project ID  |
  | fromDate  | String | 是       | 无     | 开始日期 | Start Date  |
  | toDate    | String | 是       | 无     | 结束日期 | End Date    |

- 响应体

  ```json
  {
      "code": 0,
      "message": null,
      "data": {
          "versionUploadCount": 315,
          "versionDownloadCount": 280,
          "userUploadCount": 35,
          "userDownloadCount": 30,
          "dailyStatisticsDetails": [
              {
                  "date": "2022-04-08",
                  "uploadCount": 45,
                  "downloadCount": 40
              },
              {
                  "date": "2022-04-09",
                  "uploadCount": 45,
                  "downloadCount": 40
              },
              {
                  "date": "2022-04-10",
                  "uploadCount": 45,
                  "downloadCount": 40
              },
              {
                  "date": "2022-04-11",
                  "uploadCount": 45,
                  "downloadCount": 40
              },
              {
                  "date": "2022-04-12",
                  "uploadCount": 45,
                  "downloadCount": 40
              },
              {
                  "date": "2022-04-13",
                  "uploadCount": 45,
                  "downloadCount": 40
              },
              {
                  "date": "2022-04-14",
                  "uploadCount": 45,
                  "downloadCount": 40
              }
          ]
      },
      "traceId": null
  }
  ```

- data字段说明

  | 字段                   | 类型     | 说明           | Description                          |
  | ---------------------- | -------- | -------------- | ------------------------------------ |
  | versionUploadCount     | long     | 制品上传总数   | total uploads                        |
  | versionDownloadCount   | long     | 制品下载总数   | total downloads                      |
  | userUploadCount        | long     | 上传用户数     | total upload users                   |
  | userDownloadCount      | long     | 下载用户数     | total download users                 |
  | dailyStatisticsDetails | [object] | 每日具体统计数 | detailed statistics                  |
  | date                   | String   | 具体日期       | specific day                         |
  | uploadCount            | long     | 具体日期上传数 | count of uploads in a specific day   |
  | downloadCount          | long     | 具体日期下载数 | count of downloads in a specific day |





## 查询项目依赖源仓库下载量排行

- API：GET /repository/api/project/statistics/download/rank

- API名称：get_repository_download_rank

- 功能说明：

  - 中文：查询制品包下载量排行（降序排序）
  - English: get package download rank

- 请求体 此接口无请求体

- 请求字段说明

  | 字段      | 类型   | 是否必须 | 默认值 | 说明     | Description |
  | --------- | ------ | -------- | ------ | -------- | ----------- |
  | projectId | String | 是       | 无     | 项目ID   | Project ID  |
  | fromDate  | String | 是       | 无     | 开始日期 | Start Date  |
  | toDate    | String | 是       | 无     | 结束日期 | End Date    |

- 响应体

  ```json
  {
      "code": 0,
      "message": null,
      "data": [
          {
              "packageName": "common-client-canway",
              "downloadCount": 50
          },
          {
              "packageName": "api-ticket-blueking",
              "downloadCount": 40
          },
          {
              "packageName": "devops-service-okhttp",
              "downloadCount": 30
          },
          {
              "packageName": "net.canway.devops.boot.gradle.plugin",
              "downloadCount": 20
          },
          {
              "packageName": "sdk-license",
              "downloadCount": 10
          }
      ],
      "traceId": null
  }
  ```

- data字段说明

  | 字段          | 类型   | 说明   | Description     |
  | ------------- | ------ | ------ | --------------- |
  | packageName   | String | 包名   | package name    |
  | downloadCount | long   | 下载量 | total downloads |



## 查询项目二进制仓库统计数据

- API：GET /repository/api/project/statistics/node/summary

- API名称：get_repository_project_node_statistics

- 功能说明：

  - 中文：查询项目通用文件统计数据
  - English: get project node statistics

- 请求体 此接口无请求体

- 请求字段说明

  | 字段      | 类型   | 是否必须 | 默认值 | 说明     | Description | 格式       |
  | --------- | ------ | -------- | ------ | -------- | ----------- | ---------- |
  | projectId | String | 是       | 无     | 项目ID   | Project ID  |            |
  | fromDate  | String | 是       | 无     | 开始日期 | Start Date  | yyyy-MM-dd |
  | toDate    | String | 是       | 无     | 结束日期 | End Date    | yyyy-MM-dd |

- 响应体

  ```json
  {
      "code": 0,
      "message": null,
      "data": {
          "nodeUploadCount": 315,
          "nodeDownloadCount": 280,
          "userUploadCount": 35,
          "userDownloadCount": 30,
          "dailyStatisticsDetails": [
              {
                  "date": "2022-05-05",
                  "uploadCount": 45,
                  "downloadCount": 40
              },
              {
                  "date": "2022-05-06",
                  "uploadCount": 45,
                  "downloadCount": 40
              },
              {
                  "date": "2022-05-07",
                  "uploadCount": 45,
                  "downloadCount": 40
              },
              {
                  "date": "2022-05-08",
                  "uploadCount": 45,
                  "downloadCount": 40
              },
              {
                  "date": "2022-05-09",
                  "uploadCount": 45,
                  "downloadCount": 40
              },
              {
                  "date": "2022-05-10",
                  "uploadCount": 45,
                  "downloadCount": 40
              },
              {
                  "date": "2022-05-11",
                  "uploadCount": 45,
                  "downloadCount": 40
              }
          ]
      },
      "traceId": null
  }
  ```

- data字段说明

  | 字段                   | 类型     | 说明             | Description                          |
  | ---------------------- | -------- | ---------------- | ------------------------------------ |
  | nodeUploadCount        | long     | 通用文件上传总数 | total uploads                        |
  | nodeDownloadCount      | long     | 通用文件下载总数 | total downloads                      |
  | userUploadCount        | long     | 上传用户数       | total upload users                   |
  | userDownloadCount      | long     | 下载用户数       | total download users                 |
  | dailyStatisticsDetails | [object] | 每日具体统计数   | detailed statistics                  |
  | date                   | String   | 具体日期         | specific day                         |
  | uploadCount            | long     | 具体日期上传数   | count of uploads in a specific day   |
  | downloadCount          | long     | 具体日期下载数   | count of downloads in a specific day |





## 查询项目二进制仓库下载量排行

- API：GET /repository/api/project/statistics/node/download/rank

- API名称：get_repository_node_download_rank

- 功能说明：

  - 中文：查询二进制仓库文件下载量排行（降序排序）
  - English: get node download rank

- 请求体 此接口无请求体

- 请求字段说明

  | 字段      | 类型   | 是否必须 | 默认值 | 说明     | Description | 格式       |
  | --------- | ------ | -------- | ------ | -------- | ----------- | ---------- |
  | projectId | String | 是       | 无     | 项目ID   | Project ID  |            |
  | fromDate  | String | 是       | 无     | 开始日期 | Start Date  | yyyy-MM-dd |
  | toDate    | String | 是       | 无     | 结束日期 | End Date    | yyyy-MM-dd |

- 响应体

  ```json
  {
      "code": 0,
      "message": null,
      "data": [
          {
              "repoName": "generic-repo",
              "fullPath": "/config/common.yaml",
              "name": "common.yaml",
              "downloadCount": 50
          },
          {
              "repoName": "testrepo",
              "fullPath": "/test.zip",
              "name": "test.zip",
              "downloadCount": 40
          },
          {
              "repoName": "pipe-repo",
              "fullPath": "/cpack/codecc.json",
              "name": "codecc.json",
              "downloadCount": 30
          },
          {
              "repoName": "generic-repo",
              "fullPath": "/script/bkrepo/upgrade.sh",
              "name": "upgrade.sh",
              "downloadCount": 20
          },
          {
              "repoName": "dev",
              "fullPath": "/doc/52/doc_ce.tar.gz",
              "name": "doc_ce.tar.gz",
              "downloadCount": 10
          }
      ],
      "traceId": null
  }
  ```

- data字段说明

  | 字段          | 类型   | 说明             | Description     |
  | ------------- | ------ | ---------------- | --------------- |
  | repoName      | String | 仓库名           | repo name       |
  | fullPath      | String | 在该仓库的全路径 | full path       |
  | name          | String | 文件名           | file name       |
  | downloadCount | long   | 下载量           | total downloads |

  

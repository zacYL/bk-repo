# 包版本接口

[TOC]

## 查询包版本详情（以NPM为例）

- API: GET /ext/version/detail/{projectId}/{repoName}?packageKey=npm://pkg&version=1.6.0

- API 名称: version_detail

- 功能说明：

  - 中文：查询版本详情
  - English：version detail

- 请求体
  此接口请求体为空

- 请求字段说明

  | 字段       | 类型   | 是否必须 | 默认值 | 说明      | Description        |
  | ---------- | ------ | -------- | ------ | --------- | ------------------ |
  | projectId  | string | 是       | 无     | 项目名称  | project name       |
  | repoName   | string | 是       | 无     | 仓库名称  | repo name          |
  | packageKey | string | 是       | 无     | 包唯一key | package unique key |
  | version    | string | 是       | 无     | 版本名称  | version name       |

- 响应体

  ```json
  {
      "code": 0,
      "message": null,
      "data": {
          "basic": {
              "version": "1.6.0",
              "fullPath": "/pkg/-/pkg-1.6.0.tgz",
              "size": 364,
              "sha256": "449eb4f00df4c39596d2810d9dfa783bfba2b210025cd29d27bd037c99ff05b2",
              "md5": "1ce80f5952640f1dfe51f98c492c1b22",
              "stageTag": [],
              "projectId": "test",
              "repoName": "repo-npm",
              "downloadCount": 0,
              "createdBy": "admin",
              "createdDate": "2022-05-19T10:29:46.624",
              "lastModifiedBy": "admin",
              "lastModifiedDate": "2022-05-19T10:29:46.624",
              "readme": null
          },
          "metadata": [
              {
                  "key": "name",
                  "value": "pkg",
                  "system": false,
                  "description": null
              },
              {
                  "key": "pkg-version-metadata-v2",
                  "value": "pkg-version-metadata-v2",
                  "system": false,
                  "description": "v2"
              }
          ]，
          "dependencyInfo": {
              "dependencies": [
                  {
                      "name": "pkg",
                      "version": "^1.5.0"
                  }
              ],
              "devDependencies": [],
              "dependents": [
                  "pkg"
              ]
          }
      },
      "traceId": ""
  }
  ```

- metadata字段说明：

  | 字段        | 类型    | 说明             | Description        |
  | ----------- | ------- | ---------------- | ------------------ |
  | key         | string  | 元数据键         | metadata key       |
  | value       | String  | 元数据值         | metadata value     |
  | system      | Boolean | 是否为系统元数据 | is system metadata |
  | description | String  | 元数据描述       | description        |

## 创建/更新元数据接口（V4.5.0之前）

- API：POST /api/metadata/package/{projectId}/{repoName}
- API 名称：create/update package version metadata
- 功能说明：
  - 中文：创建/更新包版本的元数据
  - English：create/update package version metadata
  - 权限：仓库更新权限
- 请求体：

```
{
    "packageKey":"npm://pkg",
    "version":"1.6.0",
    "metadata":{
        "key1":"value1",
        "key2":"value2"
    }
}
```

- 请求字段说明：

| 字段       | 类型   | 是否必须 | 默认值 | 说明       | DESCRIPTION |
| ---------- | ------ | -------- | ------ | ---------- | ----------- |
| packageKey | String | 是       | 无     | 包唯一标识 | package key |
| version    | String | 是       | 无     | 版本       | version     |
| metadata   | Object | 是       | 无     | 元数据     | metadata    |

- 响应体

```
{
    "code": 0,
    "message": null,
    "data": null,
    "traceId": ""
}
```

## 创建/更新元数据接口（V4.5.0之后）

- API：POST /api/metadata/package/{projectId}/{repoName}
- API 名称：create/update package version metadata
- 功能说明：
  - 中文：创建/更新包版本的元数据
  - English：create/update package version metadata
  - 权限：仓库更新权限
- 请求体：

```
{
    "packageKey":"npm://pkg",
    "version":"1.6.0",
    "versionMetadata":[
        {
            "key":"key1",
            "value":"value1",
            description:"desc1"
        },
        {
        	"key":"key2",
            "value":"value2",
            description:"desc2"
        }
    ]
    
}
```

- 请求字段说明：

| 字段       | 类型   | 是否必须 | 默认值 | 说明       | DESCRIPTION |
| ---------- | ------ | -------- | ------ | ---------- | ----------- |
| packageKey | String | 是       | 无     | 包唯一标识 | package key |
| version    | String | 是       | 无     | 版本       | version     |
| metadata   | Object | 是       | 无     | 元数据     | metadata    |

- 响应体

```
{
    "code": 0,
    "message": null,
    "data": null,
    "traceId": ""
}
```

